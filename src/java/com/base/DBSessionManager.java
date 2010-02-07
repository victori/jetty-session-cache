package com.base;

import com.base.cache.DBCache;
import com.base.cache.IDistributedCache;

public class DBSessionManager extends CacheSessionManager {
	private String userName;
	private String password;
	private String jdbcUrl;
	private String driver;

	public DBSessionManager(final String jdbcUrl, final String userName, final String password, final String driver,
			final String poolName) {
		super(poolName);
		this.jdbcUrl = jdbcUrl;
		this.userName = userName;
		this.password = password;
		this.driver = driver;
	}

	@Override
	protected IDistributedCache newClient(final String poolName) {
		return new DBCache(getJdbcUrl(), getUserName(), getPassword(), getDriver(), poolName);
	}

	protected String getUserName() {
		return userName;
	}

	protected void setUserName(final String userName) {
		this.userName = userName;
	}

	protected String getPassword() {
		return password;
	}

	protected void setPassword(final String password) {
		this.password = password;
	}

	protected String getJdbcUrl() {
		return jdbcUrl;
	}

	protected void setJdbcUrl(final String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	protected String getDriver() {
		return driver;
	}

	protected void setDriver(final String driver) {
		this.driver = driver;
	}

}
