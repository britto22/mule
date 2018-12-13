/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utilities for creating new events without copying all of its the internal state.
 *
 * @since 4.1.4
 */
public class EventQuickCopyInternalParams implements InternalEvent, DeserializationPostInitialisable {

  /**
   * Creates a new {@link CoreEvent} based on an existing {@link CoreEvent} instance and a {@link Map} of
   * {@link InternalEvent#getInternalParameters()}.
   * <p>
   * This is functionally the same as building a new {@link CoreEvent} setting its {@link InternalEvent#getInternalParameters()},
   * but avoids copying the whole event.
   *
   * @return new {@link CoreEvent} instance.
   */
  public static InternalEvent quickCopy(CoreEvent event, Map<String, Object> internalParameters) {
    return (event instanceof InternalEvent)
        ? new EventQuickCopyInternalParams((InternalEvent) event, internalParameters)
        : InternalEvent.builder(event).internalParameters(internalParameters).build();
  }

  private final InternalEvent event;
  private final Map<String, Object> internalParameters;

  private transient LazyValue<BindingContext> bindingContextBuilder =
      new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));

  public EventQuickCopyInternalParams(InternalEvent event, Map<String, Object> internalParameters) {
    this.event = event;
    this.internalParameters = internalParameters;
  }

  @Override
  public BaseEventContext getContext() {
    return event.getContext();
  }

  @Override
  public MuleSession getSession() {
    return event.getSession();
  }

  @Override
  public ReplyToHandler getReplyToHandler() {
    return event.getReplyToHandler();
  }

  @Override
  public Object getReplyToDestination() {
    return event.getReplyToDestination();
  }

  @Override
  public byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException {
    return event.getMessageAsBytes(muleContext);
  }

  @Override
  public Object transformMessage(DataType outputType, MuleContext muleContext) throws MessageTransformerException {
    return event.transformMessage(outputType, muleContext);
  }

  @Override
  public String getMessageAsString(MuleContext muleContext) throws MuleException {
    return event.getMessageAsString(muleContext);
  }

  @Override
  public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
    return event.getMessageAsString(encoding, muleContext);
  }

  @Override
  public boolean isNotificationsEnabled() {
    return event.isNotificationsEnabled();
  }

  @Override
  public SecurityContext getSecurityContext() {
    return event.getSecurityContext();
  }

  @Override
  public Optional<GroupCorrelation> getGroupCorrelation() {
    return event.getGroupCorrelation();
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    return event.getFlowCallStack();
  }

  @Override
  public Map<String, TypedValue<?>> getVariables() {
    return event.getVariables();
  }

  @Override
  public Message getMessage() {
    return event.getMessage();
  }

  @Override
  public Optional<Authentication> getAuthentication() {
    return event.getAuthentication();
  }

  @Override
  public Optional<Error> getError() {
    return event.getError();
  }

  @Override
  public Optional<ItemSequenceInfo> getItemSequenceInfo() {
    return event.getItemSequenceInfo();
  }

  @Override
  public String getCorrelationId() {
    return event.getCorrelationId();
  }

  @Override
  public String getLegacyCorrelationId() {
    return event.getLegacyCorrelationId();
  }

  @Override
  public BindingContext asBindingContext() {
    return bindingContextBuilder.get();
  }

  @Override
  public Map<String, ?> getInternalParameters() {
    if (event.getInternalParameters().isEmpty()) {
      return internalParameters;
    }

    final Map<String, Object> resolvedParams = new HashMap<>(event.getInternalParameters());
    resolvedParams.putAll(internalParameters);
    return resolvedParams;
  }

  @Override
  public <T> T getInternalParameter(String key) {
    final Object outerValue = internalParameters.get(key);

    return outerValue != null
        ? (T) outerValue
        : event.getInternalParameter(key);
  }

  /**
   * Invoked after deserialization. This is called when the marker interface {@link DeserializationPostInitialisable} is used.
   * This will get invoked after the object has been deserialized passing in the current MuleContext.
   *
   * @param muleContext the current muleContext instance
   * @throws MuleException if there is an error initializing
   */
  @SuppressWarnings({"unused"})
  private void initAfterDeserialisation(MuleContext muleContext) throws MuleException {
    bindingContextBuilder = new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));
  }

}
