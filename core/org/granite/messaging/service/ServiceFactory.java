/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.messaging.service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.granite.config.flex.Destination;
import org.granite.config.flex.Factory;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.util.ClassUtil;
import org.granite.util.XMap;

import flex.messaging.messages.RemotingMessage;

/**
 * @author Franck WOLFF
 */
public abstract class ServiceFactory {

	private static final Logger log = Logger.getLogger(ServiceFactory.class);
    private static final ReentrantLock lock = new ReentrantLock();

    private ServiceExceptionHandler serviceExceptionHandler;

    public static ServiceFactory getFactoryInstance(RemotingMessage request) throws ServiceException {

        GraniteContext context = GraniteContext.getCurrentInstance();

        // Simplified lookup of service factory, if configured in web.xml
        String serviceFactoryClass = context.getInitialisationMap().get("granite.ServiceFactory");
        if (serviceFactoryClass != null) {
            log.debug(">> Found factory class from web.xml: %s", serviceFactoryClass);

            return getServiceFactory(context, serviceFactoryClass);
        }

        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        log.debug(">> Finding factoryId for messageType: %s and destinationId: %s", messageType, destinationId);

        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
        	throw new ServiceException("Destination not found: " + destinationId);
        String factoryId = destination.getProperties().get("factory");

        log.debug(">> Found factoryId: %s", factoryId);

        Map<String, Object> cache = context.getApplicationMap();
        String key = ServiceFactory.class.getName() + '.' + factoryId;

        return getServiceFactory(cache, context, factoryId, key);
    }

    /**
     * Simplified lookup method that does not need servicesConfig which occasionally returns illegal destination that has no factory set.
     * <p/>
     * This method uses factory class defined in web.xml init param. Factories defined like this will receive an empty XMap in their configure call, since no services config file is being used.
     * <p/>
     * It does cache the resulting factory, but does not use a lock to create it. This means that first time, multiple service factories might be created. But since they are of the same class and that factory is assumed to be stateless, that only causes minimal overhead and no side effects.
     */
    private static ServiceFactory getServiceFactory(GraniteContext context, String serviceFactoryClass) {
        Map<String, Object> cache = context.getApplicationMap();

        ServiceFactory factory = (ServiceFactory) cache.get(serviceFactoryClass);
        if (factory == null) {
            try {
                Class<? extends ServiceFactory> clazz = ClassUtil.forName(serviceFactoryClass, ServiceFactory.class);
                factory = clazz.newInstance();
                factory.configure(XMap.EMPTY_XMAP);
            } catch (Exception e) {
                throw new ServiceException("Could not instantiate factory: " + serviceFactoryClass, e);
            }
            cache.put(serviceFactoryClass, factory);
            log.debug(">> Instantiated and cached factory from web.xml: %s, %s", serviceFactoryClass, factory);
        }

        return factory;
    }

    private static ServiceFactory getServiceFactory(Map<String, Object> cache, GraniteContext context, String factoryId, String key) {
        lock.lock();
        try {
            ServiceFactory factory = (ServiceFactory)cache.get(key);
            if (factory == null) {

                log.debug(">> No cached factory for: %s", factoryId);

                Factory config = context.getServicesConfig().findFactoryById(factoryId);
                if (config == null)
                    config = getDefaultFactoryConfig();
                try {
                    Class<? extends ServiceFactory> clazz = ClassUtil.forName(config.getClassName(), ServiceFactory.class);
                    factory = clazz.newInstance();
                    factory.configure(config.getProperties());
                } catch (Exception e) {
                    throw new ServiceException("Could not instantiate factory: " + factory, e);
                }
                cache.put(key, factory);
            }
            else
                log.debug(">> Found a cached factory for: %s", factoryId);

            log.debug("<< Returning factory: %s", factory);

            return factory;
        } finally {
            lock.unlock();
        }
    }

    private static Factory getDefaultFactoryConfig() {
        return Factory.DEFAULT_FACTORY;
    }

    public void configure(XMap properties) throws ServiceException {

        log.debug(">> Configuring factory with: %s", properties);

        // service exception handler
        String sServiceExceptionHandler = properties.get("service-exception-handler");
    	String enableLogging = properties.get("enable-exception-logging");
        if (sServiceExceptionHandler != null) {
            try {
            	if (Boolean.TRUE.toString().equals(enableLogging) || Boolean.FALSE.toString().equals(enableLogging))
                    this.serviceExceptionHandler = (ServiceExceptionHandler)ClassUtil.newInstance(sServiceExceptionHandler.trim(),
                    		new Class<?>[] { boolean.class }, new Object[] { Boolean.valueOf(enableLogging) });
            	else
            		this.serviceExceptionHandler = (ServiceExceptionHandler)ClassUtil.newInstance(sServiceExceptionHandler.trim());
            } catch (Exception e) {
                throw new ServiceException("Could not instantiate service exception handler: " + sServiceExceptionHandler, e);
            }
        }
        else {
        	if (Boolean.TRUE.toString().equals(enableLogging) || Boolean.FALSE.toString().equals(enableLogging))
        		this.serviceExceptionHandler = new DefaultServiceExceptionHandler(Boolean.valueOf(enableLogging));
        	else
        		this.serviceExceptionHandler = new DefaultServiceExceptionHandler();
        }

        log.debug("<< Configuring factory done: %s", this);
    }

    public abstract ServiceInvoker<?> getServiceInstance(RemotingMessage request) throws ServiceException;

    public ServiceExceptionHandler getServiceExceptionHandler() {
        return serviceExceptionHandler;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String append) {
        return super.toString() + " {" +
            (append != null ? append : "") +
            "\n  serviceExceptionHandler: " + serviceExceptionHandler +
        "\n}";
    }
}
