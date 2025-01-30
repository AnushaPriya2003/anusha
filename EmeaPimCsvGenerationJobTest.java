package com.whirlpoolcorp.digitalplatform.aem.core.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.whirlpoolcorp.digitalplatform.aem.core.constants.EMEAConstants;
import com.whirlpoolcorp.digitalplatform.aem.core.constants.WCMConstants;
import com.whirlpoolcorp.digitalplatform.aem.core.services.EmeaCsvGenerationService;
import com.whirlpoolcorp.digitalplatform.aem.core.utils.ResourceResolverUtils;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ ResourceResolverUtils.class })
public class EmeaPimCsvGenerationJobTest {

	@InjectMocks
	private EmeaPimCsvGenerationJob emeaPimCsvGenerationJob;

	@Mock
	private EmeaPimCsvGenerationJob.Config config;

	@Mock
	private EmeaCsvGenerationService emeaCsvGenerationService;

	@Mock
	private ResourceResolverFactory resourceResolverFactory;

	@Mock
	private ResourceResolver resourceResolver;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		// Mock for ResurceResolver from ResourceResolverFactory
		Map<String, Object> resourceResolverParams = new HashMap<>();
		resourceResolverParams.put(ResourceResolverFactory.SUBSERVICE, WCMConstants.ASSET_ADMIN);
		when(ResourceResolverUtils.initServiceResourceResolver(resourceResolverFactory, resourceResolverParams))
				.thenReturn(resourceResolver);

		when(config.languages_generic_list_path()).thenReturn(EMEAConstants.LANGUAGES_GENERIC_LIST_PATH);
		when(config.asset_url_mapping()).thenReturn(EMEAConstants.DEFAULT_URL_MAPPING);
		when(config.denied_paths()).thenReturn(new String[0]);
	}

	@Test
	public void testRun() {
		when(config.disabled()).thenReturn(false);
		emeaPimCsvGenerationJob.activate(config);
		emeaPimCsvGenerationJob.run();
		verify(emeaCsvGenerationService, times(1)).createEmeaPimCsv(eq(resourceResolver),
				eq(EMEAConstants.LANGUAGES_GENERIC_LIST_PATH), any(), anyMap());
	}

	@Test
	public void testRunDisabled() {
		when(config.disabled()).thenReturn(true);
		emeaPimCsvGenerationJob.activate(config);
		emeaPimCsvGenerationJob.run();
		verify(emeaCsvGenerationService, never()).createEmeaPimCsv(eq(resourceResolver), anyString(), any(), anyMap());
	}

	@Test
	public void testRunNullConfig() {
		emeaPimCsvGenerationJob.activate(null);
		emeaPimCsvGenerationJob.run();
		verify(emeaCsvGenerationService, never()).createEmeaPimCsv(eq(resourceResolver), anyString(), any(), anyMap());

	}
}
