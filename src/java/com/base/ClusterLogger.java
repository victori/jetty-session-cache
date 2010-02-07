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

import java.io.IOException;
import java.io.Writer;

import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.util.TypeUtil;

public class ClusterLogger extends NCSARequestLog {

	public ClusterLogger() {
		super();
	}

	public ClusterLogger(final String fileName) {
		super(fileName);
	}

	// Add server name and port to request log to see what deamon serviced what.
	@Override
	protected void logExtended(final Request request, final Response response, final Writer writer) throws IOException {
		super.logExtended(request, response, writer);
		String host = request.getConnection().getConnector().getHost();
		if (host == null) {
			host = request.getLocalAddr();
		}
		int port = request.getConnection().getConnector().getLocalPort();
		writer.write(" \"" + host + ":" + port + "\" ");
		writer.write(TypeUtil.toString(System.currentTimeMillis() - request.getTimeStamp()));
	}
}
