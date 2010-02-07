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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.log.Log;

public class FileSessionManager extends AbstractSessionManager {
	private static final long serialVersionUID = 1L;
	private String fileLocation;
	private String CREATED_ID = "_file_created_time";

	public FileSessionManager(final String location) {
		fileLocation = location;
	}

	@Override
	public void doStart() throws Exception {
		super.doStart();

		File location = new File(fileLocation);
		if (!location.exists()) {
			location.mkdirs();
		}
	}

	@Override
	public void doStop() throws Exception {
		super.doStop();
	}

	protected class Session extends AbstractSessionManager.Session {
		private Map map;
		private boolean _dirty = false;

		public Session(final HttpServletRequest arg0) {
			super(arg0);
			initValues();
			map.put(CREATED_ID, getCreationTime());
		}

		public Session(final Map map, final String key) {
			super((Long) map.get(CREATED_ID), key);
			this.map = map;
			initValues();
		}

		@Override
		public String getId() throws IllegalStateException {
			return getClusterId();
		}

		@Override
		protected void complete() {
			super.complete();
			try {
				if (_dirty) {
					persistSession(this);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.warn("Issue saving session to filecache");
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

	@Override
	protected void addSession(final AbstractSessionManager.Session arg0) {
		persistSession(arg0);
	}

	public void persistSession(final AbstractSessionManager.Session arg0) {
		synchronized (this) {
			willPassivate(arg0);

			FileOutputStream fs = null;
			ObjectOutputStream oos = null;
			File sessFile = null;
			try {
				Map sMap = ((Session) arg0).getMap();
				sessFile = new File(fileLocation + "/" + arg0.getId());
				fs = new FileOutputStream(sessFile);
				oos = new ObjectOutputStream(fs);
				oos.writeObject(sMap);
				oos.flush();
			} catch (Exception e) {
				Log.warn("Error serializing session");
			} finally {
				try {
					if (fs != null) {
						fs.close();
					}
					if (oos != null) {
						oos.close();
					}
				} catch (Exception e) {
					Log.warn("Failed to close open session files.");
				}
			}
			didActivate(arg0);
		}
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
		Session sess = null;
		synchronized (this) {
			FileInputStream in = null;
			ClassLoadingObjectInputStream ois = null;
			//ObjectInputStream ois = null;
			try {
				File sessFile = new File(fileLocation + "/" + arg0);
				in = new FileInputStream(sessFile);
				ois = new ClassLoadingObjectInputStream(in);
				//ois = new ObjectInputStream(in);
				Object o = ois.readObject();
				if (o != null) {
					sess = new Session((Map) o, arg0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (ois != null) {
						ois.close();
					}
				} catch (Exception e) {
				}

			}
		}
		return sess;
	}

	/**
	 * ClassLoadingObjectInputStream
	 * 
	 * 
	 */
	protected class ClassLoadingObjectInputStream extends ObjectInputStream {
		public ClassLoadingObjectInputStream(final java.io.InputStream in) throws IOException {
			super(in);
		}

		public ClassLoadingObjectInputStream() throws IOException {
			super();
		}

		@Override
		public Class resolveClass(final java.io.ObjectStreamClass cl) throws IOException, ClassNotFoundException {
			try {
				String classname = cl.getName();
				Class clazz = null;
				if (classname.equals("byte")) {
					clazz = byte.class;
				} else if (classname.equals("short")) {
					clazz = short.class;
				} else if (classname.equals("int")) {
					clazz = int.class;
				} else if (classname.equals("long")) {
					clazz = long.class;
				} else if (classname.equals("float")) {
					clazz = float.class;
				} else if (classname.equals("double")) {
					clazz = double.class;
				} else if (classname.equals("boolean")) {
					clazz = boolean.class;
				} else if (classname.equals("char")) {
					clazz = char.class;
				} else {
					ClassLoader loader = Thread.currentThread().getContextClassLoader();
					if (loader == null)
					{
						loader = FileSessionManager.class.getClassLoader();
					}
					clazz = Class.forName(classname,false,loader);
					//clazz = loader.loadClass(classname);
				}
				return clazz;
			} catch (ClassNotFoundException e) {
				return super.resolveClass(cl);
			}
		}
	}

	@Override
	public Map getSessionMap() {
		return null;
	}

	@Override
	public int getSessions() {
		return new File(fileLocation).list().length;
	}

	@Override
	protected void invalidateSessions() {
	}

	@Override
	protected Session newSession(final HttpServletRequest arg0) {
		return new Session(arg0);
	}

	@Override
	protected void removeSession(final String arg0) {
		synchronized (this) {
			File f = new File(fileLocation + "/" + arg0);
			if (f.exists()) {
				f.delete();
			}
		}
	}

}
