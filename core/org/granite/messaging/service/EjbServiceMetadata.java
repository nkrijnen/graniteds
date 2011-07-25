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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.granite.util.ClassUtil;
import org.granite.util.XMap;

/**
 * @author Franck WOLFF
 */
public class EjbServiceMetadata {

	private boolean stateful = false;
	private final Map<Method, Boolean> removeMethods = new HashMap<Method, Boolean>();
	
	/**
	 * Default constructor. Should only be used by the externalization mechanism.
	 */
	public EjbServiceMetadata() {
	}
	
	public EjbServiceMetadata(Class<?> scannedClass, Class<?> invokeeClass) {
		stateful = scannedClass.isAnnotationPresent(Stateful.class);
		
		if (stateful) {
			for (Method method : scannedClass.getMethods()) {
				Remove remove = method.getAnnotation(Remove.class);
				if (remove != null) {
					try {
						method = invokeeClass.getMethod(method.getName(), method.getParameterTypes());
						removeMethods.put(method, Boolean.valueOf(remove.retainIfException()));
					} catch (Exception e) {
						// ignore (invokee interface may not expose this remove method)...
					}
				}
			}
		}
	}
	
	public EjbServiceMetadata(XMap properties, Class<?> invokeeClass) {
		stateful = properties.containsKey("ejb-stateful");
		
		if (stateful) {
			for (XMap removeMethod : properties.getAll("ejb-stateful/remove-method")) {
				
				String signature = removeMethod.get("signature");
				if (signature == null)
					throw new ServiceException("Missing signature in remove-method declaration: " + properties);
				
				Boolean retainIfException = Boolean.valueOf(removeMethod.get("retain-if-exception"));

				try {
					removeMethods.put(ClassUtil.getMethod(invokeeClass, signature), retainIfException);
				}
				catch (NoSuchMethodException e) {
					throw new ServiceException("Could not find method: " + invokeeClass.getName() + "." + signature);
				}
			}
		}
	}

	public boolean isStateful() {
		return stateful;
	}
	
	public boolean isRemoveMethod(Method method) {
		return removeMethods.containsKey(method);
	}
	
	public boolean getRetainIfException(Method method) {
		return removeMethods.get(method).booleanValue();
	}
}
