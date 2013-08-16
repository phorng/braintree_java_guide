package com.braintree.guide;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.commons.io.FileUtils;

import spark.Request;
import spark.Response;
import spark.Route;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Subscription;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.braintreegateway.exceptions.NotFoundException;
import com.braintreegateway.WebhookNotification;

public class App {

    private static BraintreeGateway gateway = new BraintreeGateway(
        Environment.SANDBOX,
        "use_your_merchant_id",
        "use_your_public_key",
        "use_your_private_key"
    );

    private static String renderHtml(String pageName) {
        try {
            return FileUtils.readFileToString(new File(pageName));
        } catch (IOException e) {
            return "Couldn't find " + pageName;
        }
    }

    public static void main(String[] args) {
        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("text/html");
                return renderHtml("views/braintree.html");
            }
        });

        post(new Route("/create_customer") {
            @Override
            public Object handle(Request request, Response response) {
                CustomerRequest customerRequest = new CustomerRequest()
                    .firstName(request.queryParams("first_name"))
                    .lastName(request.queryParams("last_name"))
                    .creditCard()
                        .billingAddress()
                            .postalCode(request.queryParams("postal_code"))
                            .done()
                        .number(request.queryParams("number"))
                        .expirationMonth(request.queryParams("month"))
                        .expirationYear(request.queryParams("year"))
                        .cvv(request.queryParams("cvv"))
                        .done();

                Result<Customer> result = gateway.customer().create(customerRequest);

                response.type("text/html");
                if (result.isSuccess()) {
                  return "<h2>Customer created with name: " + result.getTarget().getFirstName() + " " + result.getTarget().getLastName() + "</h2>" +
                  "<a href=\"/subscriptions?id=" + result.getTarget().getId() + "\">Click here to sign this Customer up for a recurring payment</a>";
                } else {
                  return "<h2>Error: " + result.getMessage() + "</h2>";
                }
            }
        });

        get(new Route("/subscriptions") {
            @Override
            public Object handle(spark.Request request, Response response) {
                try {
                  Customer customer = gateway.customer().find(request.queryParams("id"));
                  String paymentMethodToken = customer.getCreditCards().get(0).getToken();

                  SubscriptionRequest req = new SubscriptionRequest()
                      .paymentMethodToken(paymentMethodToken)
                      .planId("test_plan_1");

                  Result<Subscription> result = gateway.subscription().create(req);

                  response.type("text/html");
                  return "<h1>Subscription Status</h1>" + result.getTarget().getStatus();
                } catch (NotFoundException e) {
                  return "<h1>No customer found for id: " + request.queryParams("id") + "</h1>";
                }
            }
        });

        get(new Route("/webhooks") {
          @Override
          public Object handle(spark.Request request, Response response) {
            response.type("text/html");
            return gateway.webhookNotification().verify(request.queryParams("bt_challenge"));
          }
        });

        post(new Route("/webhooks") {
          @Override
          public Object handle(spark.Request request, Response response) {
            WebhookNotification webhookNotification = gateway.webhookNotification().parse(
              request.queryParams("bt_signature"),
              request.queryParams("bt_payload")
              );
            System.out.println("[Webhook Received " + webhookNotification.getTimestamp().getTime() + "] | Kind: " + webhookNotification.getKind() + " | Subscription: " + webhookNotification.getSubscription().getId());
            response.status(200);
            return("");
          }
        });
    }
}
