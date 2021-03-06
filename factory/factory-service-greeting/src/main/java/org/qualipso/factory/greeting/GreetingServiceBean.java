package org.qualipso.factory.greeting;

import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.ws.annotation.EndpointConfig;
import org.jboss.wsf.spi.annotation.WebContext;
import org.qualipso.factory.FactoryException;
import org.qualipso.factory.FactoryNamingConvention;
import org.qualipso.factory.FactoryResource;
import org.qualipso.factory.FactoryResourceIdentifier;
import org.qualipso.factory.FactoryResourceProperty;
import org.qualipso.factory.binding.BindingService;
import org.qualipso.factory.binding.PathHelper;
import org.qualipso.factory.core.CoreService;
import org.qualipso.factory.core.CoreServiceException;
import org.qualipso.factory.eventqueue.entity.Event;
import org.qualipso.factory.greeting.entity.Name;
import org.qualipso.factory.indexing.IndexableContent;
import org.qualipso.factory.indexing.IndexableDocument;
import org.qualipso.factory.indexing.IndexingService;
import org.qualipso.factory.indexing.IndexingServiceException;
import org.qualipso.factory.membership.MembershipService;
import org.qualipso.factory.membership.MembershipServiceException;
import org.qualipso.factory.notification.NotificationService;
import org.qualipso.factory.notification.NotificationServiceException;
import org.qualipso.factory.security.pap.PAPService;
import org.qualipso.factory.security.pap.PAPServiceException;
import org.qualipso.factory.security.pap.PAPServiceHelper;
import org.qualipso.factory.security.pep.PEPService;

/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @author Benjamin Dreux (benjiiiiii@gmail.com)
 * @date 11 june 2009
 */
@Stateless(name = GreetingService.SERVICE_NAME, mappedName = FactoryNamingConvention.SERVICE_PREFIX + GreetingService.SERVICE_NAME)
@WebService(endpointInterface = "org.qualipso.factory.greeting.GreetingService", targetNamespace = FactoryNamingConvention.SERVICE_NAMESPACE
        + GreetingService.SERVICE_NAME, serviceName = GreetingService.SERVICE_NAME)
@WebContext(contextRoot = FactoryNamingConvention.WEB_SERVICE_ROOT_MODULE_CONTEXT + "-" + GreetingService.SERVICE_NAME, urlPattern = FactoryNamingConvention.WEB_SERVICE_URL_PATTERN_PREFIX
        + GreetingService.SERVICE_NAME)
@SOAPBinding(style = Style.RPC)
@SecurityDomain(value = "JBossWSDigest")
@EndpointConfig(configName = "Standard WSSecurity Endpoint")
public class GreetingServiceBean implements GreetingService{

    private static Log logger = LogFactory.getLog(GreetingServiceBean.class);

    private BindingService binding;
    private PEPService pep;
    private PAPService pap;
    private NotificationService notification;
    private MembershipService membership;
    private SessionContext ctx;
    private EntityManager em;
    private IndexingService indexing;
    private CoreService core;

    public GreetingServiceBean() {
    }

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return this.em;
    }

    @Resource
    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
    }

    @EJB
    public void setBindingService(BindingService binding) {
        this.binding = binding;
    }

    public BindingService getBindingService() {
        return this.binding;
    }
    @EJB
    public void setIndexingService(IndexingService indexing) {
        this.indexing = indexing;
    }

    public IndexingService getIndexingService() {
        return this.indexing;
    }

    @EJB
    public void setPEPService(PEPService pep) {
        this.pep = pep;
    }

    public PEPService getPEPService() {
        return this.pep;
    }

    @EJB
    public void setPAPService(PAPService pap) {
        this.pap = pap;
    }

    public PAPService getPAPService() {
        return this.pap;
    }

    @EJB
    public void setNotificationService(NotificationService notification) {
        this.notification = notification;
    }

    public NotificationService getNotificationService() {
        return this.notification;
    }

    @EJB
    public void setMembershipService(MembershipService membership) {
        this.membership = membership;
    }

    public MembershipService getMembershipService() {
        return this.membership;
    }
    
    @EJB
    public void setCoreService(CoreService core){
    	this.core = core;
    }
    
    public CoreService getCoreService(){
    	return this.core;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createName(String path, String value) throws GreetingServiceException {
    
        logger.info("createName(...) called");
        logger.debug("params : path=" + path + ", value=" + value);
        
        try {
            //Checking if the connected user has the permission to create a resource giving pep : 
            //  - the profile path of the connected user (caller)
            //  - the parent of the path (we check the 'create' permission on the parent of the given path)
            //  - the name of the permission to check ('create')
            String caller = membership.getProfilePathForConnectedIdentifier();
            pep.checkSecurity(caller, PathHelper.getParentPath(path), "create");
            
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE CREATION OR METHOD CALL
            Name name = new Name();
            name.setId(UUID.randomUUID().toString());
            name.setValue(value);
            em.persist(name);
            //END OF EXTERNAL INVOCATION
            
            //Binding the external resource in the naming using the generated resource ID : 
            binding.bind(name.getFactoryResourceIdentifier(), path);
            
            //Need to set some properties on the node : 
            binding.setProperty(path, FactoryResourceProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
            binding.setProperty(path, FactoryResourceProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
            binding.setProperty(path, FactoryResourceProperty.AUTHOR, caller);
            
            //Need to create a new security policy for this resource : 
            //Giving the caller the Owner permission (aka all permissions)
            String policyId = UUID.randomUUID().toString();
            pap.createPolicy(policyId, PAPServiceHelper.buildOwnerPolicy(policyId, caller, path));
            pap.createPolicy(UUID.randomUUID().toString(), PAPServiceHelper.buildPolicy(policyId, caller,path, new String[]{"read"}));
            
            //Setting security properties on the node : 
            binding.setProperty(path, FactoryResourceProperty.OWNER, caller);
            binding.setProperty(path, FactoryResourceProperty.POLICY_ID, policyId);
            
            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "create"), ""));

            //Using the indexing service to index the name newly created
            indexing.index(getServiceName(), path);
        } catch ( Exception e ) {
            ctx.setRollbackOnly();
            logger.error("unable to create the name at path " + path, e);
            throw new GreetingServiceException("unable to create the name at path " + path, e);
        }
    }

    /**
    * Give name at the given path.
    * @param pepCheck true to make the check.
    * @param path the path to the name.
    * @throws GreetingServiceException if the name can't be find.
    */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private Name readName(String path, boolean pepCheck) throws GreetingServiceException {
    try {
            //Checking if the connected user has the permission to read the resource giving pep : 
            String caller = membership.getProfilePathForConnectedIdentifier();
            
            if(pepCheck)
            pep.checkSecurity(caller, path, "read");
            
            //Performing a lookup in the naming to recover the Resource Identifier 
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            //Checking if this resource identifier is really a resource managed by this service (a Hello resource)
            checkResourceType(identifier, Name.RESOURCE_NAME);
        
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS
            Name name = em.find(Name.class, identifier.getId());
            if ( name == null ) {
                throw new GreetingServiceException("unable to find a name for id " + identifier.getId());
            }
            name.setResourcePath(path);
            //END OF EXTERNAL SERVICE INVOCATION

            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "read"), ""));
            
            return name;
        } catch ( Exception e ) {
            logger.error("unable to read the name at path " + path, e);
            throw new GreetingServiceException("unable to read the name at path " + path, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Name readName(String path) throws GreetingServiceException {
        logger.info("readName(...) called");
        logger.debug("params : path=" + path);
        return readName(path, true);
        
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateName(String path, String value) throws GreetingServiceException {
        logger.info("updateName(...) called");
        logger.debug("params : path=" + path + ", value=" + value);
        
        try {
            //Checking if the connected user has the permission to update the resource giving pep : 
            String caller = membership.getProfilePathForConnectedIdentifier();
            pep.checkSecurity(caller, path, "update");
            
            //Performing a lookup in the naming to recover the Resource Identifier 
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            //Checking if this resource identifier is really a resource managed by this service (a Hello resource)
            checkResourceType(identifier, Name.RESOURCE_NAME);
        
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS
            Name name = em.find(Name.class, identifier.getId());
            if ( name == null ) {
                throw new GreetingServiceException("unable to find a name for id " + identifier.getId());
            }
            name.setValue(value);
            em.merge(name);
            //END OF EXTERNAL SERVICE INVOCATION
        
            //Need to set some properties on the node : 
            binding.setProperty(path, FactoryResourceProperty.LAST_UPDATE_TIMESTAMP, System.currentTimeMillis() + "");
            
            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "update"), ""));
            
            //Using the indexing service to reindex the name newly updated
            indexing.reindex(getServiceName(), path);

        } catch ( Exception e ) {
         //   ctx.setRollbackOnly();
            logger.error("unable to update the name at path " + path, e);
            throw new GreetingServiceException("unable to update the name at path " + path, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteName(String path) throws GreetingServiceException {
        logger.info("deleteName(...) called");
        logger.debug("params : path=" + path);
        
        try {
            //Checking if the connected user has the permission to delete the resource giving pep : 
            String caller = membership.getProfilePathForConnectedIdentifier();
            pep.checkSecurity(caller, path, "delete");
            
            //Performing a lookup in the naming to recover the Resource Identifier 
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            //Checking if this resource identifier is really a resource managed by this service (a Hello resource)
            checkResourceType(identifier, Name.RESOURCE_NAME);
        
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS
            Name name = em.find(Name.class, identifier.getId());
            if ( name == null ) {
                throw new GreetingServiceException("unable to find a name for id " + identifier.getId());
            }
            em.remove(name);
            //END OF EXTERNAL SERVICE INVOCATION
            
            //Removing the security policy of this node : 
            String policyId = binding.getProperty(path, FactoryResourceProperty.POLICY_ID, false);
            pap.deletePolicy(policyId);

            //Unbinding the resource from this path : 
            binding.unbind(path);
            
            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "delete"), ""));

            //Using the indexing service to unindex the name 
            indexing.remove(getServiceName(), path);

        } catch ( Exception e ) {
           // ctx.setRollbackOnly();
            logger.error("unable to delete the name at path " + path, e);
            throw new GreetingServiceException("unable to delete the name at path " + path, e);
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String sayHello(String path) throws GreetingServiceException {
        logger.info("sayHello(...) called");
        logger.debug("params : path=" + path);
        
        try {
            //Checking if the connected user has the permission to say-hello the resource giving pep : 
            String caller = membership.getProfilePathForConnectedIdentifier();
            pep.checkSecurity(caller, path, "say-hello");
            
            //Performing a lookup in the naming to recover the Resource Identifier 
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            //Checking if this resource identifier is really a resource managed by this service (a Hello resource)
            checkResourceType(identifier, Name.RESOURCE_NAME);
        
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS
            Name name = em.find(Name.class, identifier.getId());
            if ( name == null ) {
                throw new GreetingServiceException("unable to find a name for id " + identifier.getId());
            }
            //END OF EXTERNAL SERVICE INVOCATION
            
            //Building hello message : 
            String message = "Hello dear " + name.getValue() + " !!";

            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "say-hello"), ""));
            
            return message;
        } catch ( Exception e ) {
          //  ctx.setRollbackOnly();
            logger.error("unable to say hello to the name at path " + path, e);
            throw new GreetingServiceException("unable to say hello to the name at path " + path, e);
        }
    }

    
    
    private void checkResourceType(FactoryResourceIdentifier identifier, String resourceType) throws MembershipServiceException {
        if ( !identifier.getService().equals(getServiceName()) ) {
            throw new MembershipServiceException("resource identifier " + identifier + " does not refer to service " + getServiceName());
        }
        if ( !identifier.getType().equals(resourceType) ) {
            throw new MembershipServiceException("resource identifier " + identifier + " does not refer to a resource of type " + resourceType);
        }
    }

    @Override
    public String[] getResourceTypeList() {
        return RESOURCE_TYPE_LIST;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

   /** 
    * Read name with an optional pep check.
    * @param path path to the given resource.
    * @param pepCheck  true to make the check.
    * @return a factoryResource if it can be find.
    */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private FactoryResource findResource (String path, boolean pepCheck) throws FactoryException {
        try {
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            if ( !identifier.getService().equals(GreetingService.SERVICE_NAME) ) {
                throw new GreetingServiceException("Resource " + identifier + " is not managed by " + GreetingService.SERVICE_NAME);
            }
            
            if ( identifier.getType().equals(Name.RESOURCE_NAME) ) {
                return readName(path, pepCheck);
            } 
            
            throw new GreetingServiceException("Resource " + identifier + " is not managed by Greeting Service");
            
        } catch (Exception e) {
            logger.error("unable to find the resource at path " + path, e);
            throw new GreetingServiceException("unable to find the resource at path " + path, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FactoryResource findResource(String path) throws FactoryException {
        logger.info("findResource(...) called");
        logger.debug("params : path=" + path);
        
        return findResource(path, true);
    }
    
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void readNameWithUser(String path, String caller) throws GreetingServiceException {
        logger.info("readNameWithUser(...) called");
        logger.debug("params : path=" + path);
        
        try {
            //Checking if the connected user has the permission to read the resource giving pep : 
            //pep.checkSecurity(caller, path, "read");
            
            //Performing a lookup in the naming to recover the Resource Identifier 
            FactoryResourceIdentifier identifier = binding.lookup(path);
            
            //Checking if this resource identifier is really a resource managed by this service (a Hello resource)
            checkResourceType(identifier, Name.RESOURCE_NAME);
        
            //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS
            Name name = em.find(Name.class, identifier.getId());
            if ( name == null ) {
                throw new GreetingServiceException("unable to find a name for id " + identifier.getId());
            }
            name.setResourcePath(path);
            //END OF EXTERNAL SERVICE INVOCATION

            //Using the notification service to throw an event : 
            notification.throwEvent(new Event(path, caller, Name.RESOURCE_NAME, Event.buildEventType(GreetingService.SERVICE_NAME, Name.RESOURCE_NAME, "read"), ""));
            
        } catch ( Exception e ) {
            logger.error("unable to read the name at path " + path, e);
            throw new GreetingServiceException("unable to read the name at path " + path, e);
        }
    }

	@Override
	public IndexableDocument getIndexableDocument(String path) throws IndexingServiceException{
	try{
        // use internal findResource
		Name name = (Name)findResource(path, false);
		IndexableContent content = new IndexableContent();
		content.addContentPart(name.getValue());

 
        IndexableDocument doc = new IndexableDocument();

		doc.setIndexableContent(content);
		doc.setResourceService(getServiceName());
		doc.setResourceShortName(name.getValue());
		doc.setResourceType(Name.RESOURCE_NAME);
		doc.setResourcePath(path);
		doc.setResourceFRI(name.getFactoryResourceIdentifier());

		return doc;
	} catch (Exception e) {
            ctx.setRollbackOnly();
            logger.error("unable to convert name to IndexableContent " + path, e);
            throw new IndexingServiceException("unable to convert name to IndexableContent" + path, e);
        }
	}

    @Override
    public void throwNullEvent() throws NotificationServiceException {
        notification.throwEvent(null);
    }

    @Override
    public void throw2SameEvent(String path) throws NotificationServiceException, MembershipServiceException {
        String caller = membership.getProfilePathForConnectedIdentifier();
        Event e = new Event(path, caller, "Name", "greeting.name.create", "");
        notification.throwEvent(e);
        notification.throwEvent(e);
    }

    @Override
    public void throwFacticeEvent() throws NotificationServiceException {
        notification.throwEvent(new Event("/unexist/path", "toto", "Name", "greeting", ""));
    }

	@Override
	public void createFolder(String path,String name) throws GreetingServiceException {
		try {
			core.createFolder(path, name, "greeting folder");
		} catch (CoreServiceException e) {
			logger.error("unable to create folder "+name+" at "+path);
			e.printStackTrace();
			throw new GreetingServiceException(e.getMessage());
		}		
	}
	
	@Override
	public void giveAutorization(String path, String user, String[] actions) throws GreetingServiceException{
		String policyId = UUID.randomUUID().toString();
		try {
			pap.createPolicy(policyId, PAPServiceHelper.buildPolicy(policyId, user, path, actions));
		} catch (PAPServiceException e) {
			logger.error("unable to build policy to "+user+" on "+path);
			e.printStackTrace();
			throw new GreetingServiceException(e.getMessage());
		}
	}
	@Override
    public void deleteFolder(String path) throws GreetingServiceException{
		try {
			core.deleteFolder(path);
		} catch (CoreServiceException e) {
			logger.error("unable to delete folder "+path);
			e.printStackTrace();
			throw new GreetingServiceException(e.getMessage());
		}
	}
    /*
     * public Event createEvent(String string, String caller,String name, String
     * arg1, String arg2) throws GreetingServiceException{ Event ev = new
     * Event(string, caller, name, arg1, arg2); return ev; }
     * 
     * 
     * public String throwEventOK() throws GreetingServiceException{ String path
     * = "/names/sheldon"; logger.info("thowEventOk(...) called");
     * logger.debug("params : path=" + path);
     * 
     * try { //Checking if the connected user has the permission to thowEventOk
     * the resource giving pep : String caller =
     * membership.getProfilePathForConnectedIdentifier(); //
     * pep.checkSecurity(caller, path, "thowEventOk");
     * 
     * //Performing a lookup in the naming to recover the Resource Identifier
     * //FactoryResourceIdentifier identifier = binding.lookup(path);
     * 
     * //Checking if this resource identifier is really a resource managed by
     * this service (a Hello resource) // checkResourceType(identifier, "Name");
     * 
     * //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD CALLS //
     * Name name = em.find(Name.class, identifier.getId()); // if ( name == null
     * ) { // throw new GreetingServiceException("unable to find a name for id "
     * + identifier.getId()); // } //END OF EXTERNAL SERVICE INVOCATION
     * 
     * /* //Building hello message : String message = "Hello dear " +
     * name.getValue() + " !!";
     * 
     * 
     * //Using the notification service to throw an event : // String pathqueu =
     * eq.getPath();// path de la que // String owner =
     * eq.getOwner();//propritaire de la queu
     * 
     * //create name resource createName(path, "Sheldon Cooper"); String
     * policyId = UUID.randomUUID().toString(); pap.createPolicy(policyId,
     * PAPServiceHelper.buildOwnerPolicy(policyId, caller, path));
     * 
     * Event ev = new Event(path, caller, "Name", "greeting.name.thowEventOK",
     * "");
     * 
     * //if(owner.equals(caller)){ notification.throwEvent(ev); //}
     * 
     * return path; } catch ( Exception e ) { ctx.setRollbackOnly();
     * logger.error("unable to thowEventOk", e); throw new
     * GreetingServiceException("unable to thowEventOk", e); }
     * 
     * 
     * 
     * 
     * /* String caller = membership.getProfilePathForConnectedIdentifier();
     * Event ev = new Event("/path/resource/", caller, "Name",
     * "hello.name.create", ""); //return ev; return ""; }
     * 
     * 
     * public String throwEventKO() throws GreetingServiceException{
     * 
     * // logger.info("thowEventKo(...) called"); //
     * logger.debug("params : path=" + path); // // try { // //Checking if the
     * connected user has the permission to thowEventOk the resource giving pep
     * : // String caller = membership.getProfilePathForConnectedIdentifier();
     * // pep.checkSecurity(caller, path, "thowEventKo"); // // //Performing a
     * lookup in the naming to recover the Resource Identifier //
     * FactoryResourceIdentifier identifier = binding.lookup(path); // //
     * //Checking if this resource identifier is really a resource managed by
     * this service (a Hello resource) // checkResourceType(identifier, "Name");
     * // // //STARTING SPECIFIC EXTERNAL SERVICE RESOURCE LOADING OR METHOD
     * CALLS // Name name = em.find(Name.class, identifier.getId()); // if (
     * name == null ) { // throw new
     * GreetingServiceException("unable to find a name for id " +
     * identifier.getId()); // } // //END OF EXTERNAL SERVICE INVOCATION // //
     * /* //Building hello message : // String message = "Hello dear " +
     * name.getValue() + " !!"; //
     */
    //
    // //Using the notification service to throw an event :
    // //membership.createProfile(identifier, fullname, email, accountStatus);
    // String caller1 = "caller";
    // Event ev = new Event(path, caller1, "Name", "greeting.name.thowEventKo",
    // "");
    // //notification.throwEvent(ev);
    //            
    // return "";
    // } catch ( Exception e ) {
    // ctx.setRollbackOnly();
    // logger.error("unable to thowEventKO to the name at path " + path, e);
    // throw new
    // GreetingServiceException("unable to thowEventKo to the name at path " +
    // path, e);
    // }

    /*
     * String caller = "call"; Event ev = new Event("/path/resource/", caller,
     * "Name", "hello.name.create", ""); return ev;
     * 
     * String path = "/names/sheldon"; logger.info("thowEventKO(...) called");
     * logger.debug("params : path=" + path);
     * 
     * try { //Checking if the connected user has the permission to thowEventOk
     * the resource giving pep : String caller =
     * membership.getProfilePathForConnectedIdentifier();
     * membership.createProfile("toto", "toto titi", "toto@gmail.com", 1);
     * //create name resource createName(path, "Sheldon Cooper"); String
     * policyId = UUID.randomUUID().toString(); //pap.createPolicy(policyId,
     * PAPServiceHelper.buildOwnerPolicy(policyId, caller, path));
     * pap.createPolicy(policyId,PAPServiceHelper.buildPolicy(policyId,
     * "/profile/toto", path, new String[]{"read"})); Event ev = new Event(path,
     * caller, "Name", "greeting.name.thowEventKO", "");
     * 
     * //if(owner.equals(caller)){ notification.throwEvent(ev); //}
     * 
     * return path; } catch ( Exception e ) { ctx.setRollbackOnly();
     * logger.error("unable to thowEventKO", e); throw new
     * GreetingServiceException("unable to thowEventKO", e); }
     * 
     * }
     * 
     * 
     * 
     * /* public void createQueue(String path) throws GreetingServiceException{
     * eqs.createQueue("path");
     * 
     * }
     */
}
