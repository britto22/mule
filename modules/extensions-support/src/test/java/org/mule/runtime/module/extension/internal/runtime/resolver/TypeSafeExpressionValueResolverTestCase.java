/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.verification.VerificationMode;

public class TypeSafeExpressionValueResolverTestCase extends AbstractMuleContextTestCase {

  private static final String HELLO_WORLD = "Hello World!";
  private static final MetadataType STRING = new JavaTypeLoader(currentThread().getContextClassLoader()).load(String.class);

  @Rule
  public ExpectedException expected = none();

  private ExtendedExpressionManager expressionManager;

  @Override
  protected void doSetUp() throws Exception {
    muleContext = spy(muleContext);
    expressionManager = spy(muleContext.getExpressionManager());

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject("_muleExpressionManager", expressionManager);
  }

  @Test
  public void expressionLanguageWithoutTransformation() throws Exception {
    assertResolved(getResolver("#['Hello ' ++ payload]", STRING)
        .resolve(ValueResolvingContext.from(eventBuilder(muleContext).message(of("World!")).build())), HELLO_WORLD, times(1));
  }

  @Test
  public void expressionTemplateWithoutTransformation() throws Exception {
    assertResolved(getResolver("#['Hello $(payload)']", STRING)
        .resolve(ValueResolvingContext.from(eventBuilder(muleContext).message(of("World!")).build())), HELLO_WORLD, times(1));
  }

  @Test
  public void constant() throws Exception {
    assertResolved(getResolver("Hello World!", STRING)
        .resolve(ValueResolvingContext.from(eventBuilder(muleContext).message(of(HELLO_WORLD)).build())), HELLO_WORLD, never());
  }

  @Test
  public void expressionWithTransformation() throws Exception {
    assertResolved(getResolver("#[true]", STRING)
        .resolve(ValueResolvingContext.from(eventBuilder(muleContext).message(of(HELLO_WORLD)).build())), "true", times(1));
  }

  @Test
  public void templateWithTransformation() throws Exception {
    assertResolved(getResolver("#['tru$('e')']", STRING)
        .resolve(ValueResolvingContext.from(eventBuilder(muleContext).message(of(HELLO_WORLD)).build())), "true", times(1));
  }

  @Test
  public void nullExpression() throws Exception {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Expression cannot be blank or null");
    getResolver(null, STRING);
  }

  @Test
  public void blankExpression() throws Exception {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Expression cannot be blank or null");
    getResolver(EMPTY, STRING);
  }

  @Test
  public void nullExpectedType() throws Exception {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("expected type cannot be null");
    getResolver("#[payload]", null);
  }

  private void assertResolved(Object resolvedValue, Object expected, VerificationMode expressionManagerVerificationMode) {
    assertThat(resolvedValue, instanceOf(String.class));
    assertThat(resolvedValue, equalTo(expected));
    verifyExpressionManager(expressionManagerVerificationMode);
  }

  private void verifyExpressionManager(VerificationMode mode) {
    verify(expressionManager, mode).evaluate(anyString(), any(DataType.class), any(BindingContext.class));

    // These 2 calls below are called from within the evaluate method above
    verify(expressionManager, mode).evaluate(anyString(), any(DataType.class), any(BindingContext.class),
                                             isNull(CoreEvent.class));
    verify(expressionManager, mode).evaluate(anyString(), any(DataType.class), any(BindingContext.class),
                                             isNull(CoreEvent.class), isNull(ComponentLocation.class), anyBoolean());
  }

  private <T> ValueResolver<T> getResolver(String expression, MetadataType expectedType) throws Exception {
    TypeSafeExpressionValueResolver<T> valueResolver = new TypeSafeExpressionValueResolver(expression,
                                                                                           getType(expectedType).orElse(null),
                                                                                           expectedType);
    muleContext.getInjector().inject(valueResolver);
    valueResolver.setExtendedExpressionManager(expressionManager);
    valueResolver.setTransformationService(muleContext.getTransformationService());
    valueResolver.initialise();
    return valueResolver;
  }
}
