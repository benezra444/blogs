/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wicketinaction;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.json.JsonFunction;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.resource.JQueryPluginResourceReference;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.template.PackageTextTemplate;

/**
 * A button that uses Handlebars JavaScript templating library to
 * update HTML element's content
 */
public abstract class HandlebarsButton<T> extends AjaxButton
{
	/**
	 * Uses JQueryPluginResourceReference to specify that it depends on jQuery.
	 * This way jQuery will be delivered before handlebars.js
	 */
	private static final JavaScriptResourceReference HANDLEBARS_JS =
			new JQueryPluginResourceReference(HandlebarsButton.class, "handlebars-1.0.0.beta.6.js");

	/**
	 * The body of a Wicket.Ajax's #onSuccess() listener that uses the Ajax response
	 * which is passed with the special argument name 'data'
	 */
	private final JsonFunction onSuccessFunction;

	/**
	 * Constructor.
	 *
	 * @param id the component id
	 * @param templateId the id of the Handlebars template
	 *      E.g. <script id="THIS_ID" type="text/x-handlebars-template">
	 * @param targetSelector the css selector to use to find the HTML element that
	 *      should be updated with the populated Handlebars template
	 */
	public HandlebarsButton(String id, String templateId, String targetSelector)
	{
		super(id);

		Args.notEmpty(templateId, "templateId");
		Args.notEmpty(targetSelector, "targetSelector");

		PackageTextTemplate blogJs = new PackageTextTemplate(HandlebarsButton.class, "HandlebarsButton.js.tmpl");
		Map<String, Object> variables = Generics.newHashMap();
		variables.put("templateId", templateId);
		variables.put("targetSelector", targetSelector);
		this.onSuccessFunction = new JsonFunction(blogJs.asString(variables));
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);

		// let Wicket.Ajax/jQuery know that the result is JSON so it will parse it for you
		attributes.setDataType("json");

		// tell Wicket.Ajax to not try to process the Ajax response because it is not the normal <ajax-response>
		attributes.setWicketAjaxResponse(false);

		// register the onSuccess listener that will execute Handlebars logic
		AjaxCallListener listener = new AjaxCallListener() {
			@Override
			public CharSequence getSuccessHandler(Component component)
			{
				// returns an instance of JsonFunction, so Wicket will render it as is
				return onSuccessFunction;
			}
		};

		// the following handlers are not JsonFunction and they will be wrapped in JsonFunction by Wicket and the
		// appropriate parameters will be passed.

		// For the 'before' handler it looks like: function(attrs, jqXHR, settings){Wicket.Log.info('[Wicket Ajax 6 demo]: executing a before handler');}
		listener.onBefore("Wicket.Log.info('[Wicket Ajax 6 demo]: executing a before handler');");

		// For the 'complete' handler it looks like: function(attrs, jqXHR, textStatus){Wicket.Log.info('[Wicket Ajax 6 demo]: executing a complete handler');}
		listener.onComplete("Wicket.Log.info('[Wicket Ajax 6 demo]: executing a complete handler. Status: ' + textStatus);");

		// change the return to 'false' and the Ajax call wont be executed at all.
		listener.onPrecondition("return true;");

		attributes.getAjaxCallListeners().add(listener);
	}

	/**
	 * Serializes the object to its JSON representation that will be used as
	 * an Ajax response.
	 * This method that should be implemented by the client of this component.
	 *
	 * @param object the object to serialize
	 * @return the JSON representation of the passed object
	 */
	protected abstract CharSequence asJson(T object);

	/**
	 * Serializes the form's model object to JSON and delivers it to the browser.
	 *
	 * @param target the Ajax request handler. Since we work with JSON we just ignore it.
	 * @param form the form that contains this button. Keeps the POJO as a model object.
	 */
	@Override
	protected final void onSubmit(AjaxRequestTarget target, Form<?> form)
	{
		T object = ((Form<T>) form).getModelObject();

		String json = asJson(object).toString();

		// schedule a request handler that will serve the JSON response
		TextRequestHandler jsonHandler = new TextRequestHandler("application/json", "UTF-8", json);

		// replace AjaxRequestHandler with the JSON one
		getRequestCycle().replaceAllRequestHandlers(jsonHandler);

	}

	/**
	 * Contribute Handlebars.js
	 *
	 * @param response the response that writes the page headers
	 */
	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(HANDLEBARS_JS));
	}
}
