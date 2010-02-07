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

import com.base.cache.AsyncMemcache;
import com.base.cache.IDistributedCache;

public class EhMemcachedSessionManager extends MemcachedSessionManager {
	private static final int DEFAULT_TTL = 1800; // 30 minutes..
	private int localttl = DEFAULT_TTL;

	public EhMemcachedSessionManager(final String[] servers, final String poolName) {
		this(servers, poolName,DEFAULT_TTL);
	}

	public EhMemcachedSessionManager(final String[] servers, final String poolName,final int localttl) {
		super(servers, poolName);
		setLocalttl(localttl);
	}

	@Override
	protected IDistributedCache newClient(final String poolName) {
		return new AsyncMemcache(getServers(),poolName,true,getLocalttl());
	}

	public void setLocalttl(final int localttl) {
		this.localttl = localttl;
	}

	public int getLocalttl() {
		return localttl;
	}
}
