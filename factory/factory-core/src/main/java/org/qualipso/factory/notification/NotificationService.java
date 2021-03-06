/*
 * Qualipso Funky Factory
 * Copyright (C) 2006-2010 INRIA
 * http://www.inria.fr - molli@loria.fr
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation. See the GNU
 * Lesser General Public License in LGPL.txt for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * Initial authors :
 *
 * Jérôme Blanchard / INRIA
 * Christophe Bouthier / INRIA
 * Pascal Molli / Nancy Université
 * Gérald Oster / Nancy Université
 */
package org.qualipso.factory.notification;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.qualipso.factory.FactoryNamingConvention;
import org.qualipso.factory.FactoryService;
import org.qualipso.factory.eventqueue.entity.Event;
import org.qualipso.factory.notification.entity.Rule;

/**
 * 
 * NotificationService is the interface class for the Notification Core
 * Component. There are four methods you can call from the Notification Service.
 * This service allow you to match a rule to a specific queue. You can:
 * <ul>
 * <li>Register a rule to a queue.</li>
 * <li>Unregister rule from a queue.</li>
 * <li>Throw an Event.</li>
 * <li>Get the list of the rules registered.</li>
 * </ul>
 * A rule is defined by a tuple:
 * <ul>
 * <li>subject</li>
 * <li>object</li>
 * <li>target</li>
 * </ul>
 * <p>
 * A tuple (subject, object, target) example is :
 * <code>("jeff", "commit", "/p1/t1/toto")</code>. Once this tuple is registered
 * in the Notification Service, if jeff commits in <code>/p1/t1/toto</code>, the
 * rule will match this event and it will be pushed in the registered queue, but
 * if he commits in <code>/p1/t1</code>, it will not match.
 * <p>
 * You can match more generic events : <code>("j*","com*","/p1/t1/*")</code>.
 * With this rule, if jeff commits in <code>/p1/t1</code>, it will match, but if
 * jack or john also commits in <code>/p1/t1</code>, it will match as well.
 * 
 * @author Jean-Francois Grand
 * @author Nicolas HENRY
 * @author Marlène HANTZ
 * @version 11/19/2009
 */
@Remote
@WebService(name = NotificationService.SERVICE_NAME, targetNamespace = FactoryNamingConvention.SERVICE_NAMESPACE + NotificationService.SERVICE_NAME)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface NotificationService extends FactoryService {

    public static final String SERVICE_NAME = "notification";
    public static final String[] RESOURCE_TYPE_LIST = new String[] {};

    /**
     * 
     * Allows a user to register a rule to an event queue. A queue can be
     * created using the eventqueue service. Rules are tuples: (subject, object,
     * target).
     * 
     * @param subjectre
     *            the subject is the author of the action you want to register.
     * @param objectre
     *            the object of the type of the action (commit, ...).
     * @param targetre
     *            the target of the action (for example: the path to the project
     *            in which you commit, <code>/p1/t1</code>).
     * @param queuePath
     *            the path to the queue in which events matching the rules will
     *            be pushed.
     * @throws NotificationServiceException
     */
    @WebMethod
    public void register(String subjectre, String objectre, String targetre, String queuePath) throws NotificationServiceException;

    /**
     * 
     * Allows a user to unregister a rule from a queue.
     * 
     * @param subjectre
     *            the subject is the author of the action you want to
     *            unregister.
     * @param objectre
     *            objectre the object of the type of the action (commit, ...).
     * @param targetre
     *            the target of the action (for example: the path to the project
     *            in which you commit, <code>/p1/t1</code>).
     * @param queuePath
     *            the path to the queue in which events matching the rules will
     *            be pushed.
     * @throws NotificationServiceException
     */
    @WebMethod
    public void unregister(String subjectre, String objectre, String targetre, String queuePath) throws NotificationServiceException;

    /**
     * 
     * Allows to throw events from the Notification Service.
     * 
     * @param e
     *            the event to be thrown, see
     *            org.qualipso.factory.eventqueue.entity.Event.
     * @throws NotificationServiceException
     */
    @WebMethod
    public void throwEvent(Event e) throws NotificationServiceException;

    /**
     * 
     * Lists the registered rules.
     * 
     * @return a tab of the rules registered in the Notification Service
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] list() throws NotificationServiceException;

    /**
     * 
     * Lists the registered rules matching the given args. Use null instead of
     * arg to skip it. Ex:list("toto",null,null,"/p1/q1") will return a list of
     * the rules registered by toto on /p1/q1
     * 
     * @return a tab of the rules registered matching the given args
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listMatchingBy(String subject, String object, String target, String queue) throws NotificationServiceException;

    /**
     * 
     * Lists the registered rules matching the regular expressions specified.
     * Use null instead of arg to skip it. Ex:list("toto",null,null,"/p1/q1")
     * will return a list of the rules registered by toto on /p1/q1
     * 
     * @return a tab of the rules registered matching the specified regular
     *         expressions
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listRE(String subjectre, String objectre, String targetre, String queuere) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered to the eventqueue queue.
     * 
     * @return a tab of the rules registered to queue.
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByQueue(String queue) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered to the eventqueues matching the regular
     * expression queuere.
     * 
     * @return a tab of the rules registered to the queues matching the regular
     *         expression queuere.
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByQueueRE(String queuere) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered by subject.
     * 
     * @return a tab of the rules registered by subject
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listBySubject(String subject) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered by subjects matching the regular expression
     * subjectre.
     * 
     * @return a tab of the rules registered by subjects matching the regular
     *         expression subjectre
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listBySubjectRE(String subjectre) throws NotificationServiceException;

    /**
     * 
     * Lists the registered rules of type object.
     * 
     * @return a tab of the rules registered of type object
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByObject(String object) throws NotificationServiceException;

    /**
     * 
     * Lists the registered rules of types matching the regular expression
     * objectre.
     * 
     * @return a tab of the rules registered of types matching the regular
     *         expression objectre
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByObjectRE(String objectre) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered on target.
     * 
     * @return a tab of the rules registered on target
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByTarget(String target) throws NotificationServiceException;

    /**
     * 
     * Lists the rules registered on targets matching the regular expression
     * targetre.
     * 
     * @return a tab of the rules registered on the targets matching the regular
     *         expression targetre
     * @throws NotificationServiceException
     */
    @WebMethod
    public Rule[] listByTargetRE(String targetre) throws NotificationServiceException;

}
