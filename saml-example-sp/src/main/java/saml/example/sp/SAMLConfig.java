package saml.example.sp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.saml2.binding.encoding.HTTPPostEncoder;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessor;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;

@Configuration
public class SAMLConfig {

	@Bean
	public MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager() {
		return new MultiThreadedHttpConnectionManager();
	}

	@Bean
	public HttpClient httpClient() {
		return new HttpClient(multiThreadedHttpConnectionManager());
	}

	private HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine,
			ArtifactResolutionProfile artifactResolutionProfile) {
		return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile);
	}

	@Bean
	@Autowired
	public HTTPSOAP11Binding soapBinding(ParserPool parserPool) {
		return new HTTPSOAP11Binding(parserPool);
	}

	@Bean
	@Autowired
	public HTTPPostBinding httpPostBinding(ParserPool parserPool, VelocityEngine velocityEngine,
			@Value("${sp.compare_endpoints}") boolean compareEndpoints) {
		HTTPPostEncoder encoder = new HTTPPostEncoder(velocityEngine, "/templates/saml2-post-binding.vm");
		HTTPPostDecoder decoder = new HTTPPostDecoder(parserPool);
		if (!compareEndpoints) {
			decoder.setURIComparator((uri1, uri2) -> true);
		}
		return new HTTPPostBinding(parserPool, decoder, encoder);
	}

	@Bean
	@Autowired
	public HTTPRedirectDeflateBinding httpRedirectDeflateBinding(ParserPool parserPool) {
		return new HTTPRedirectDeflateBinding(parserPool);
	}

	@Bean
	@Autowired
	public HTTPSOAP11Binding httpSOAP11Binding(ParserPool parserPool) {
		return new HTTPSOAP11Binding(parserPool);
	}

	@Bean
	@Autowired
	public HTTPPAOS11Binding httpPAOS11Binding(ParserPool parserPool) {
		return new HTTPPAOS11Binding(parserPool);
	}

	@Autowired
	@Bean
	public SAMLProcessor processor(VelocityEngine velocityEngine, ParserPool parserPool,
			SpConfiguration spConfiguration, @Value("${sp.compare_endpoints}") boolean compareEndpoints) {
		ArtifactResolutionProfile artifactResolutionProfile = new ArtifactResolutionProfileImpl(httpClient());
		Collection<SAMLBinding> bindings = new ArrayList<>();
		bindings.add(httpRedirectDeflateBinding(parserPool));
		bindings.add(httpPostBinding(parserPool, velocityEngine, compareEndpoints));
		bindings.add(artifactBinding(parserPool, velocityEngine, artifactResolutionProfile));
		bindings.add(httpSOAP11Binding(parserPool));
		bindings.add(httpPAOS11Binding(parserPool));
		return new ConfigurableSAMLProcessor(bindings, spConfiguration);
	}

	@Bean
	public static SAMLBootstrap samlBootstrap() {
		return new SAMLBootstrap();
	}

	@Bean
	public SAMLDefaultLogger samlLogger() {
		return new SAMLDefaultLogger();
	}
	
	@Bean
	public SAMLContextProvider contextProvider(@Value("${sp.base_url}") String spBaseUrl) throws URISyntaxException {
		URI uri = new URI(spBaseUrl);
		SAMLContextProviderLB contextProvider = new SAMLContextProviderLB();
		contextProvider.setServerName(uri.getHost());
		contextProvider.setScheme(uri.getScheme());
		contextProvider.setContextPath("");
		if (uri.getPort() > 0) {
			contextProvider.setIncludeServerPortInRequestURL(true);
			contextProvider.setServerPort(uri.getPort());
		}
		return contextProvider;
	}

	@Bean
	public WebSSOProfileConsumer webSSOprofileConsumer() {
		WebSSOProfileConsumerImpl webSSOProfileConsumer = new WebSSOProfileConsumerImpl();
		webSSOProfileConsumer.setResponseSkew(15 * 60);
		return webSSOProfileConsumer;
	}

	@Bean
	public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	@Bean
	@Autowired
	public WebSSOProfile webSSOprofile(SAMLProcessor samlProcessor) {
		WebSSOProfileImpl webSSOProfile = new WebSSOProfileImpl();
		webSSOProfile.setProcessor(samlProcessor);
		return webSSOProfile;
	}

	@Bean
	public WebSSOProfileECPImpl ecpprofile() {
		return new WebSSOProfileECPImpl();
	}

}
