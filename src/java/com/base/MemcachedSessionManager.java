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
import com.base.cache.Memcache;

/*
 * 
 * First attempt at writing a jetty session handler.
 * I am currently using this in production, so it works.
 * 
 * Concerning session expiration, I rely on memcache's LRU algorithm to expire sessions.
 * Using this session handler will override jetty's default hashedsessionidmanager.
 * 
 */

public class MemcachedSessionManager extends CacheSessionManager {
	private List<String> servers = new ArrayList<String>();


	public MemcachedSessionManager(final String[] servers, final String poolName) {
		super(poolName);
		for (String srv : servers) {
			this.getServers().add(srv);
		}
	}

	@Override
	protected IDistributedCache newClient(final String poolName) {
		return new Memcache(getServers(), poolName);
	}

	public void setServers(final List<String> servers) {
		this.servers = servers;
	}

	public List<String> getServers() {
		return servers;
	}



}
