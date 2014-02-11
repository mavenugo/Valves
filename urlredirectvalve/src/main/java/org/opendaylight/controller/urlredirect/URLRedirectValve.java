package org.opendaylight.controller.urlredirect;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLRedirectValve  extends ValveBase {

    private static final Logger logger = LoggerFactory
            .getLogger(URLRedirectValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        IClusterGlobalServices clusterService = (IClusterGlobalServices) ServiceHelper.getGlobalInstance(
                IClusterGlobalServices.class, this);
        if (clusterService == null) {
            logger.debug("clusterService is not available");
            getNext().invoke(request, response);
            return;
        }

        if (clusterService.amICoordinator()) {
            logger.debug("am the Coordinator. No redirection needed");
            getNext().invoke(request, response);
            return;
        }

        IConnectionManager connectionManager = (IConnectionManager) ServiceHelper.getGlobalInstance(
                IConnectionManager.class, this);
        if (connectionManager == null) {
            logger.debug("ConnectionManager is not available");
            getNext().invoke(request, response);
            return;
        }
        ConnectionMgmtScheme activeScheme = connectionManager.getActiveScheme();
        if (!activeScheme.equals(ConnectionMgmtScheme.SINGLE_CONTROLLER)) {
            logger.debug("URL Redirect is supported only for SINGLE_CONTROLLER Scheme");
            getNext().invoke(request, response);
            return;
        }
        InetAddress active = clusterService.getCoordinatorAddress();
        String url = request.getRequestURL().toString();
        String baseURL = url.replace(request.getRequestURI().substring(1),"");
        url = url.replace(baseURL, request.getScheme()+"://"+active.getHostAddress()+":"+request.getLocalPort()+"/");
        StringBuffer convertedUrl = new StringBuffer(url);
        String queryString = request.getQueryString();
        if (queryString != null) convertedUrl.append ("?"+queryString);

        logger.debug("Redirected Location : "+url);
        response.setStatus (Response.SC_MOVED_TEMPORARILY);
        response.setHeader ("Location", response.encodeRedirectURL(convertedUrl.toString()));
    }
}
