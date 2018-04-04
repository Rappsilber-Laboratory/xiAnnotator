package org.rappsilber;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class xiAnnotatorTestMatchSearchMissmatch {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer();
        // create the client
        Client c = ClientBuilder.newClient();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testGetIt() {
        String responseMsg = target.path("annotate/3413/68913-53595-83402-24933/210231825/")
                .queryParam("peptide","TVTAMDVVYALK","TLYGFGG")
                .queryParam("link","4","6")
                .queryParam("custom","fragment:BLikeDoubleFragmentation;ID:4")
                .request().get(String.class);
        System.out.println(responseMsg);
        assertEquals("{\"error\":\"No Spectra\"}", responseMsg);
    }
}
