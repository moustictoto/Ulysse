package org.qualipso.factory.indexing;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.ws.annotation.EndpointConfig;
import org.jboss.wsf.spi.annotation.WebContext;

import org.qualipso.factory.FactoryNamingConvention;
import org.qualipso.factory.FactoryResourceIdentifier;
import org.qualipso.factory.membership.MembershipService;
import org.qualipso.factory.membership.MembershipServiceException;
import org.qualipso.factory.security.pep.PEPService;
import org.qualipso.factory.security.pep.PEPServiceException;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>Class which implements Indexing Service</p>
 * @author Benjamin Dreux (benjiiiiii@gmail.com)
 * @author cynthia FLORENTIN
 * @date 25 october 2009
 */

@Stateless(name = IndexingService.SERVICE_NAME, mappedName = FactoryNamingConvention.SERVICE_PREFIX + IndexingService.SERVICE_NAME)
@SecurityDomain(value = "JBossWSDigest")

public class IndexingServiceBean implements IndexingService {
	
	private static Log logger = LogFactory.getLog(IndexingServiceBean.class);
	static String indexingQueuePath = "queue/QualipsoFactory/Indexing";
	
	private PEPService pep;
	private MembershipService membership;
	private SessionContext ctx;
	private Queue indexingQueue;
	private QueueConnectionFactory queueConnectionFactory;
	private IndexI index;
	
    
    public IndexingServiceBean() {
	}

	@Resource
	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
	@EJB
	public void setPEPService(PEPService pep) {
		this.pep = pep;
	}

	public PEPService getPEPService() {
		return this.pep;
	}

	@EJB
	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public MembershipService getMembershipService() {
		return this.membership;
	}
	
	public void setIndex(IndexI index) {
		this.index = index;
	}
	public IndexI getIndex(){
		return this.index;
	}

	
	@Resource(mappedName="jms/QueueConnectionFactory")
	public void setQueueConnectionFactory(QueueConnectionFactory queueConnectionFactory){
		this.queueConnectionFactory = queueConnectionFactory;
	}
	public QueueConnectionFactory getQueueConnectionFatory(){
		return this.queueConnectionFactory;
	}
	
	@Resource(mappedName="jms/queue/indexingQueue")
	public void setIndexingQueue(Queue indexingQueue){
		this.indexingQueue = indexingQueue;
	}
	public Queue getIndexingQueue(){
		return this.indexingQueue;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void index(FactoryResourceIdentifier fri) throws IndexingServiceException {
		logger.info("index(...) called ");
		logger.debug("params : FactoryResourceIdentifier=\r\n" + fri + "\r\n}");
		String action = "index";
		
        sendMessage(action,fri);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void reindex(FactoryResourceIdentifier fri) throws IndexingServiceException {
		logger.info("reindex(...) called ");
		logger.debug("params : FactoryResourceIdentifier=\r\n" + fri + "\r\n}");
		String action = "reindex";
		
        sendMessage(action,fri);

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void remove(FactoryResourceIdentifier fri) throws IndexingServiceException {
		logger.info("remove(...) called ");
		logger.debug("params : FactoryResourceIdentifier=" + fri);
		String action = "remove";
		
        sendMessage(action,fri);

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ArrayList<SearchResult> search(String query) throws IndexingServiceException {
		logger.info("search(...) called ");
		logger.debug("params : query=" + query);
		ArrayList<SearchResult> unCheckRes = index.search(query);
		return filter(unCheckRes);
	}

	private ArrayList<SearchResult> filter(ArrayList<SearchResult> uncheckedRes) throws IndexingServiceException {
		Iterator<SearchResult> iter = uncheckedRes.iterator();
		ArrayList<SearchResult> checkedRes = new ArrayList<SearchResult>();
		try{	
			String profile = membership.getProfilePathForConnectedIdentifier();
			while(iter.hasNext()){
				SearchResult current = iter.next();
				FactoryResourceIdentifier fri = current.getResourceIdentifier();
				try{
					pep.checkSecurity(profile, fri.toString(), "read");
					checkedRes.add(current);
				}catch(PEPServiceException e){}
			}
		}catch(MembershipServiceException e){
			logger.error("Error in indexingservice when filtring searchResult", e);
            ctx.setRollbackOnly();
            throw new IndexingServiceException("Error in indexingservice when filtring searchResult", e);  
		}
		return checkedRes;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void sendMessage(String action, FactoryResourceIdentifier fri) throws IndexingServiceException{
		try{
			QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
			try{
				QueueSession queueSession = queueConnection.createQueueSession(true, javax.jms.Session.AUTO_ACKNOWLEDGE);
				try{
					QueueSender queueSender = queueSession.createSender(indexingQueue);
					try {

						Message message = queueSession.createMessage();
						message.setStringProperty("action", action);
						message.setStringProperty("uri", fri.toString());
						queueSender.send(message);
						queueSender.close();
						queueSession.close();
						queueConnection.close();
					}finally{queueSender.close();}
				}finally{queueSession.close();}
			}finally{queueConnection.close();}
		}catch(JMSException e){
			logger.error("Error in indexingservice when sending message "+ action, e);
            ctx.setRollbackOnly();
            throw new IndexingServiceException("Error in indexingservice when sending message "+ action, e);  
		}
	}


}