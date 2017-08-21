package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.impl.rev141210;

import org.opendaylight.messenger.impl.MessengerProvider;

public class MessengerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.impl.rev141210.AbstractMessengerModule {
    public MessengerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessengerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.impl.rev141210.MessengerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        // throw new java.lang.UnsupportedOperationException();
    	
    	MessengerProvider provider = new MessengerProvider();
    	
    	
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
