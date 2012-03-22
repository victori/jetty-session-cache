/*
 * Copyright 2009 Victor Igumnov <victori@fabulously40.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.base;

import com.base.cache.ICacheStat;
import com.base.cache.IDistributedCache;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CacheSessionManager extends AbstractSessionManager {
    private static final long serialVersionUID = 1L;
    private IDistributedCache cache;
    private String poolName;
    private static String CREATED_KEY = "_memcache-created-key";
    private static String UPDATED_KEY = "_memcache-updated-key";
    private static String ACCESSED_KEY = "_memcache-accessed-key";
    private static String EXPIRE_KEY = "_memcache-expire-key";
    private SessionIdManager _sessionIdManager;
    private ConcurrentHashMap<String, WeakReference<Session>> localStore;
    private final static transient Logger logger = Log.getLogger(CacheSessionManager.class.getName());

    protected abstract IDistributedCache newClient(final String poolName);

    protected String getPoolName() {
        return poolName;
    }

    public CacheSessionManager(final String poolName) {
        this.poolName = poolName;
    }

    public IDistributedCache getCache() {
        if (cache == null) {
            cache = newClient(poolName);
        }
        return cache;
    }

    protected String generateKey(final String currKey) {
        if (!currKey.startsWith(poolName)) {
            return poolName + "." + currKey;
        } else {
            return currKey;
        }
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        cache = newClient(poolName);

        Server server = getSessionHandler().getServer();
        synchronized (server) {
            _sessionIdManager = server.getSessionIdManager();
            logger.warn("Starting new manager...", null);
            if (!(_sessionIdManager instanceof CacheSessionIdManager)) {
                logger.warn("Configuring new cache session id manager...", null);
                _sessionIdManager = new CacheSessionIdManager(cache);
                setIdManager(_sessionIdManager);
                server.setSessionIdManager(_sessionIdManager);
            }
        }
        if (!_sessionIdManager.isStarted()) {
            _sessionIdManager.start();
        }
        localStore = new ConcurrentHashMap<String, WeakReference<Session>>();
    }

    public void doStop() throws Exception {
        super.doStop();
        localStore.clear();
    }

    protected class Session extends AbstractSessionManager.Session {
        private Map map;
        private boolean _dirty = false;

        public Session(final HttpServletRequest arg0) {
            super(arg0);
            // initvalues should call newAttributeMap
            initValues();
            this.map.put(CREATED_KEY, getCreationTime());
            this.map.put(UPDATED_KEY, getCreationTime());
            this.map.put(ACCESSED_KEY, getCreationTime());
            this.map.put(EXPIRE_KEY, System.currentTimeMillis() + (_dftMaxIdleSecs * 1000));
        }

        public Session(final Map map, final String key) {
            super((Long) map.get(CREATED_KEY), key);
            this.map = map;
            initValues();
            // store this session in the local cache for the duration of the render
            localStore.put(key, new WeakReference(this));
        }

        @Override
        protected void access(final long time) {
            super.access(time);
            this.map.put(ACCESSED_KEY, time);
            this.map.put(EXPIRE_KEY, time + (_dftMaxIdleSecs * 1000));
        }

        @Override
        public String getId() throws IllegalStateException {
            return getClusterId();
        }

        public long getExpireMs() {
            Long expiretime = (Long) this.map.get(EXPIRE_KEY);
            if (expiretime == null) {
                return 0;
            } else {
                return expiretime;
            }
        }

        @Override
        protected void complete() {
            super.complete();
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing local session " + this.getId(), null);
                }
                localStore.remove(this.getId());
                if (_dirty) {
                    logger.debug("saving session...", null);
                    persistSession(this);
                }
            } catch (Exception e) {
                logger.debug("Issue saving session to memcache", e);
            } finally {
                _dirty = false;
            }

        }

        @Override
        public synchronized void setAttribute(final String arg0, final Object arg1) {
            super.setAttribute(arg0, arg1);
            _dirty = true;
        }

        @Override
        public synchronized void removeAttribute(final String arg0) {
            super.removeAttribute(arg0);
            _dirty = true;
        }

        private static final long serialVersionUID = 1L;

        @Override
        protected Map newAttributeMap() {
            return map != null ? map : (map = new HashMap());
        }

        public Map getMap() {
            return map;
        }
    }

    public void persistSession(final AbstractSessionManager.Session arg0) {
        synchronized (this) {
            willPassivate(arg0);

            ObjectOutputStream oos = null;
            try {
                Map sMap = ((Session) arg0).getMap();
                sMap.put(UPDATED_KEY, System.currentTimeMillis());

                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bs);
                oos.writeObject(sMap);
                oos.flush();

                if (logger.isDebugEnabled()) {
                    logger.debug("Persisting " + arg0.getId() + " as " + generateKey(arg0.getId()), null);
                }
                cache.put(generateKey(arg0.getId()), bs.toByteArray());
            } catch (Exception e) {
                logger.warn("Error serializing session: " + e.getMessage(), e);
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                } catch (Exception e) {
                    logger.warn("Failed to close open session files.", e);
                }
            }
            didActivate(arg0);
        }
    }

    @Override
    protected void addSession(final AbstractSessionManager.Session arg0) {
        persistSession(arg0);
    }

    protected void willPassivate(final AbstractSessionManager.Session sess) {
        HttpSessionEvent event = new HttpSessionEvent(sess);
        for (Enumeration e = sess.getAttributeNames(); e.hasMoreElements();) {
            Object value = e.nextElement();
            if (value instanceof HttpSessionActivationListener) {
                HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                listener.sessionWillPassivate(event);
            }
        }
    }


    protected void didActivate(final AbstractSessionManager.Session sess) {
        HttpSessionEvent event = new HttpSessionEvent(sess);
        for (Enumeration e = sess.getAttributeNames(); e.hasMoreElements();) {
            Object value = e.nextElement();
            if (value instanceof HttpSessionActivationListener) {
                HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                listener.sessionDidActivate(event);
            }
        }
    }

    @Override
    public Session getSession(final String arg0) {

        // getSession is called many times during a render phase.
        // Therefor, we are using a localStore if possible.
        //
        // Since the ConcurrentHashMap localStore potentially could be modified
        // concurrently while we are executing this method (since no
        // synchronization occurs here), it is crucial that we never only check
        // for existence of session id in localStore. Rather, we always want to
        // extract its value immediately for local usage.
        WeakReference<Session> local_element_reference = localStore.get(arg0);
        Session sess = null;
        if (local_element_reference != null) {
            sess = local_element_reference.get();
            if (sess == null) {
                localStore.remove(arg0);
            }
        }

        if (sess != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using locally stored session " + arg0);
            }
            return sess;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("local session collected remotely " + arg0);
            }
            return loadRemoteSession(arg0);
        }

    }

    protected Session loadRemoteSession(String arg0) {
        Session sess = loadSession(arg0);
        if (sess != null) {
            didActivate(sess);
        }
        return sess;
    }

    @Override
    public Map getSessionMap() {
        return null;
    }

    @Override
    public int getSessions() {
        if (cache instanceof ICacheStat) {
            return Long.valueOf(((ICacheStat) cache).getCacheElements()).intValue();
        } else {
            return 0;
        }
    }

    @Override
    protected void invalidateSessions() {
        localStore.clear();
        // Do nothing - we don't want to remove and
        // invalidate all the sessions because this
        // method is called from doStop(), and just
        // because this context is stopping does not
        // mean that we should remove the session from
        // any other nodes
    }

    @Override
    protected Session newSession(final HttpServletRequest arg0) {
        return new Session(arg0);
    }

    @Override
    protected void removeSession(final String arg0) {
        synchronized (this) {
            localStore.remove(arg0);
            cache.remove(generateKey(arg0));
        }
    }

    public Session loadSession(final String arg0) {
        Session sess = null;
        synchronized (this) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fetching session for key " + arg0 + " as " + generateKey(arg0), null);
            }
            byte[] bytes = (byte[]) cache.get(generateKey(arg0));
            if (bytes == null) {
                return null;
            }
            ClassLoadingObjectInputStream ois = null;
            //ObjectInputStream ois = null;
            try {
                ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
                ois = new ClassLoadingObjectInputStream(bs);
                //ois = new ObjectInputStream(bs);
                Object o = ois.readObject();
                if (o != null) {
                    sess = new Session((Map) o, arg0);
                }
            } catch (Exception e) {
                logger.warn("Issue loading memcache session.", e);
            } finally {
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (Exception e) {
                }
            }
        }
        return sess;
    }
}
