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

import com.base.cache.AsyncDBCache;
import com.base.cache.ICache;
import com.base.cache.IDistributedCache;
import com.base.cache.Memcache2;

public class AsyncDBSessionManager extends DBSessionManager {
	private List<String> servers = new ArrayList<String>();

	public AsyncDBSessionManager(final String jdbcUrl, final String userName, final String password,
			final String driver, final String poolName, final String[] servers) {
		super(jdbcUrl, userName, password, driver, poolName);
		for (String srv : servers) {
			this.getServers().add(srv);
		}
	}

	protected ICache newSecondLevelCache() {
		return new Memcache2(getServers(),getPoolName());
	}

	@Override
	protected IDistributedCache newClient(final String poolName) {
		return new AsyncDBCache(getJdbcUrl(), getUserName(), getPassword(), getDriver(), poolName, newSecondLevelCache());
	}

	public void setServers(final List<String> servers) {
		this.servers = servers;
	}

	public List<String> getServers() {
		return servers;
	}

}
