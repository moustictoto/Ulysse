
package ws.factory.qualipso.org.service.project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.7-b01-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "ProjectService", targetNamespace = "http://org.qualipso.factory.ws/service/project", wsdlLocation = "http://localhost:3000/factory-service-project/project?wsdl")
public class ProjectService_Service
    extends Service
{

    private final static URL PROJECTSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(ws.factory.qualipso.org.service.project.ProjectService_Service.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = ws.factory.qualipso.org.service.project.ProjectService_Service.class.getResource(".");
            url = new URL(baseUrl, "http://localhost:3000/factory-service-project/project?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:3000/factory-service-project/project?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        PROJECTSERVICE_WSDL_LOCATION = url;
    }

    public ProjectService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ProjectService_Service() {
        super(PROJECTSERVICE_WSDL_LOCATION, new QName("http://org.qualipso.factory.ws/service/project", "ProjectService"));
    }

    /**
     * 
     * @return
     *     returns ProjectService
     */
    @WebEndpoint(name = "ProjectService")
    public ProjectService getProjectService() {
        return super.getPort(new QName("http://org.qualipso.factory.ws/service/project", "ProjectService"), ProjectService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ProjectService
     */
    @WebEndpoint(name = "ProjectService")
    public ProjectService getProjectService(WebServiceFeature... features) {
        return super.getPort(new QName("http://org.qualipso.factory.ws/service/project", "ProjectService"), ProjectService.class, features);
    }

}
