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

import com.base.cache.IDistributedCache;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class CacheSessionIdManager extends AbstractLifeCycle implements SessionIdManager {
    private IDistributedCache cache;
    private final static String __NEW_SESSION_ID = "com.base.memcacheId";
    protected final static String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";
    protected final static String SESSION_ID_RANDOM_ALGORITHM_ALT = "IBMSecureRandom";
    private final static transient Logger logger = Log.getLogger(CacheSessionIdManager.class.getName());
    private Random _random;

    public CacheSessionIdManager(final IDistributedCache client2) {
        this.cache = client2;
        if (_random == null) {
            try {
                //This operation may block on some systems with low entropy. See this page
                //for workaround suggestions:
                //http://docs.codehaus.org/display/JETTY/Connectors+slow+to+startup
                logger.warn("Init SecureRandom.",null);
                _random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM);
            }
            catch (NoSuchAlgorithmException e) {
                try {
                    _random = SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM_ALT);
                }
                catch (NoSuchAlgorithmException e_alt) {
                    logger.warn("Could not generate SecureRandom for session-id randomness", e_alt);
                    _random = new Random();
                }
            }
        }
        _random.setSeed(_random.nextLong() ^ System.currentTimeMillis() ^ hashCode() ^ Runtime.getRuntime().freeMemory());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    public void addSession(final HttpSession arg0) {
        // handled by session manager
    }

    public String getClusterId(final String arg0) {
        return arg0;
    }

    public String getNodeId(final String arg0, final HttpServletRequest arg1) {
        return arg0;
    }

    public String getWorkerName() {
        return null;
    }

    public boolean idInUse(final String arg0) {
        return cache.keyExists(arg0);
    }

    public void invalidateAll(final String arg0) {
        // once again they invalidate them selves,we store nothing in this
        // idmanager
    }

    public String newSessionId(final HttpServletRequest request, final long created) {
        synchronized (this) {
            // A requested session ID can only be used if it is in use already.
            String requested_id = request.getRequestedSessionId();

            if (requested_id != null) {
                // String cluster_id = getClusterId(requested_id);
                if (idInUse(requested_id)) {
                    return requested_id;
                }
            }

            // Else reuse any new session ID already defined for this request.
            String new_id = (String) request.getAttribute(__NEW_SESSION_ID);
            if (new_id != null && idInUse(new_id)) {
                return new_id;
            }

            // pick a new unique ID!
            String id = null;
            String ipAddress = null;
            while (id == null || id.length() == 0 || idInUse(id)) {
                // A bit more random.
                for (int i = 0; i < 2; i++) {
                    long r = _random.nextLong();
                    r ^= created;

                    // X-Real-IP wins if defined.
                    if (request != null && ipAddress == null) {
                        // Making sure to only calculate ipAddress once, if needed
                        if (request.getRemoteAddr() != null) {
                            ipAddress = request.getRemoteAddr();
                        }
                        if (request.getHeader(HttpHeaders.X_FORWARDED_FOR) != null) {
                            ipAddress = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
                        }
                        if (request.getHeader("X-Real-IP") != null) {
                            ipAddress = request.getHeader("X-Real-IP");
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("Hashing for " + ipAddress, null);
                        }
                    }
                    if (ipAddress != null) {
                        r ^= ipAddress.hashCode();
                    }
                    if (r < 0) {
                        r = -r;
                    }
                    id = (id == null) ? Long.toString(r, 36) : id + Long.toString(r, 36);
                }
                
                if(logger.isDebugEnabled()) {
                    logger.debug("Generated ID " + id, null);
                }
            }

            request.setAttribute(__NEW_SESSION_ID, id);
            return id;
        }

    }

    public void removeSession(final HttpSession arg0) {
        // handled by session manager
    }
}
