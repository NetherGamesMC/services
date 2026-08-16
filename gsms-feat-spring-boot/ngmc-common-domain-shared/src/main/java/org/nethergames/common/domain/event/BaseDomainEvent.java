package org.nethergames.common.domain.event;

import org.springframework.context.ApplicationEvent;

public class BaseDomainEvent extends ApplicationEvent implements IDomainEvent {
	public BaseDomainEvent(Object source) {
		super(source);
	}
}
