/**
 *   GRANITE DATA SERVICES
 *   Copyright (C) 2006-2013 GRANITE DATA SERVICES S.A.S.
 *
 *   This file is part of Granite Data Services.
 *
 *   Granite Data Services is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or (at your
 *   option) any later version.
 *
 *   Granite Data Services is distributed in the hope that it will be useful, but
 *   WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
 *   for more details.
 *
 *   You should have received a copy of the GNU Library General Public License
 *   along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package org.granite.test.tide.seam.action;

import org.granite.test.tide.TestDataUpdatePostprocessor;
import org.granite.tide.data.DataUpdatePostprocessor;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.ServletLifecycle;


@Name("duppFactory")
@BypassInterceptors
public class TestDuppFactory {

	@Factory(value="org.granite.tide.seam.data.dataUpdatePreprocessor", scope=ScopeType.EVENT, autoCreate=true)
	public DataUpdatePostprocessor buildDupp() {
		if (ServletLifecycle.getServletContext().getAttribute("dupp") != null)
			return new TestDataUpdatePostprocessor();
		return null;
	}
}
