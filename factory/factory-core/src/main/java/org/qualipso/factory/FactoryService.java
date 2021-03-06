/*
 *
 * Qualipso Factory
 * Copyright (C) 2006-2010 INRIA
 * http://www.inria.fr - molli@loria.fr
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of LGPL. See licenses details in LGPL.txt
 *
 * Initial authors :
 *
 * Jérôme Blanchard / INRIA
 * Pascal Molli / Nancy Université
 * Gérald Oster / Nancy Université
 *
 */
package org.qualipso.factory;


/**
 * The interface for all factory services. A factory service may manage one or more resources but can 
 * also provide functionalities which doesn't implies resources.<br/>
 * <br/>
 * Depending on the visibility of a service, it can be internal or external to the factory. An internal service
 * intend to be used only by other services whereas an external service intend to be accessible to factory users.<br/>
 * <br/>
 * A factory service must define some properties :
 * <ul>
 * <li>a unique service name, 
 * <li>the list of resource types managed by this service.
 * </ul>
 * <br/>
 * A factory service must also offer the findResource operation for a given path.<br/>
 * 
 * 
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 20 May 2009
 */
public interface FactoryService {
	
	/**
	 * @return the service name
	 */
	public abstract String getServiceName();

    /**
     * @return the list of resource types managed by this service
     */
    public abstract String[] getResourceTypeList();
    
    /**
     * @param path the path of the resource to find
     * @return the resource binded to this path
     * @throws FactoryException if the resource has not been found
     */
    public abstract FactoryResource findResource(String path) throws FactoryException;
    
}
