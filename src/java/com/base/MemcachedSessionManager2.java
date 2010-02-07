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

import java.util.ArrayList;
import java.util.List;

import com.base.cache.IDistributedCache;
import com.base.cache.Memcache2;

public class MemcachedSessionManager2 extends CacheSessionManager {
	private List<String> servers = new ArrayList<String>();

	@Override
	protected IDistributedCache newClient(final String poolName) {
		return new Memcache2(getServers(), poolName);
	}

	public MemcachedSessionManager2(final String[] servers, final String poolName) {
		super(poolName);
		for (String srv : servers) {
			this.getServers().add(srv);
		}
	}

	protected void setServers(final List<String> servers) {
		this.servers = servers;
	}

	protected List<String> getServers() {
		return servers;
	}

}
