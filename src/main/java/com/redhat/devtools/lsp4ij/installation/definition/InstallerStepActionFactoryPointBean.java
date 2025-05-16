/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.lsp4ij.installation.definition;

import com.intellij.AbstractBundle;
import com.intellij.DynamicBundle;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Server extension point bean.
 *
 * <pre>
 *   <extensions defaultExtensionNs="com.redhat.devtools.lsp4ij">
 *     <installerStepActionFactory
 *         type="exec"
 *         factoryClass="com.redhat.devtools.lsp4ij.installation.definition.steps.ExecStepActionFactory">
 *     </description>
 *   </installerStepActionFactory>
 * </extensions>
 * </pre>
 */
public class InstallerStepActionFactoryPointBean extends BaseKeyedLazyInstance<InstallerStepActionFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallerStepActionFactoryPointBean.class);

    public static final ExtensionPointName<InstallerStepActionFactoryPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.lsp4ij.installerStepActionFactory");

    /**
     * The installer step action factory type.
     */
    @Attribute("type")
    @RequiredElement
    public String type;

    /**
     * The {@link InstallerStepActionFactory} implementation used to create step action.
     */
    @Attribute("factoryClass")
    @RequiredElement
    public String factoryClass;

    @Override
    protected @Nullable String getImplementationClassName() {
        return factoryClass;
    }

}
