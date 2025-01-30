package com.whirlpoolcorp.digitalplatform.aem.core.job;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.whirlpoolcorp.digitalplatform.aem.core.constants.EMEAConstants;
import com.whirlpoolcorp.digitalplatform.aem.core.constants.WCMConstants;
import com.whirlpoolcorp.digitalplatform.aem.core.services.EmeaCsvGenerationService;
import com.whirlpoolcorp.digitalplatform.aem.core.utils.ResourceResolverUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = Runnable.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = EmeaPimCsvGenerationJob.Config.class)
public class EmeaPimCsvGenerationJob implements Runnable {

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private EmeaCsvGenerationService emeaCsvGenerationService;

	private boolean disabled = true;

	private String languagesGenericListPath;

	private String assetUrlMapping;

	private String[] deniedPaths;

	private int purgeDays;

	@Activate
	protected void activate(final EmeaPimCsvGenerationJob.Config config) {
		if (config != null) {
			this.disabled = config.disabled();
			this.languagesGenericListPath = config.languages_generic_list_path();
			this.assetUrlMapping = config.asset_url_mapping();
			this.deniedPaths = config.denied_paths();
			this.purgeDays = config.number_of_days_purge();
			LOG.info("EmeaPimCsvGenerationJob activated");
		}
	}

	public void cleanUpOldFolders(ResourceResolver resourceResolver) {
		LocalDate cutOffDate = LocalDate.parse(LocalDate.now().minusDays(purgeDays).toString(), DateTimeFormatter.ofPattern("yyyy-mm-dd"));

			for (Resource childResource : resourceResolver.getResource("/content/dam/emea/pim").getChildren()) {
				if (StringUtils.containsIgnoreCase(childResource.getResourceType(), "folder")){
				LocalDate folderDate = LocalDate.parse(childResource.getName());
				String  folderPath =childResource.getPath();
				if (folderDate.isBefore(cutOffDate)) {
					try {
						resourceResolver.delete(childResource);
						LOG.debug("Deleted folder: {}", folderPath);
						
					} catch (PersistenceException e) {
						LOG.error("Failed to delete folder: {} due to PersistenceException", folderPath, e);
					}
					
				}
			}
	}
		
}

	@Override
	public void run() {
		if (disabled) {
			LOG.error("EmeaPimCsvGenerationJob is disabled");
			return;
		}
		LOG.debug("EmeaPimCsvGenerationJob is running");

		

		ResourceResolver resourceResolver = ResourceResolverUtils.initServiceResourceResolver(resourceResolverFactory,
				WCMConstants.ASSET_ADMIN);
		

		emeaCsvGenerationService.createEmeaPimCsv(resourceResolver, languagesGenericListPath, deniedPaths,
				getAssetUrlMapper());

				cleanUpOldFolders(resourceResolver);
		ResourceResolverUtils.releaseResourceResolver(resourceResolver);

		LOG.debug("EmeaPimCsvGenerationJob is completed");
	}

	/**
	 * Method to generate assetUrlMapper map.
	 * 
	 * @return assetUrlMapper
	 */
	private Map<String, String> getAssetUrlMapper() {

		Map<String, String> assetUrlMapper = new LinkedHashMap<>();

		for (String mappedUrl : StringUtils.split(assetUrlMapping, EMEAConstants.PIPE_SYMBOL)) {
			String[] entry = StringUtils.split(mappedUrl, EMEAConstants.ASTERISK);
			assetUrlMapper.put(entry[0], entry[1]);
		}

		return assetUrlMapper;
	}

	/**
	 * EMEA CSV Generation Scheduler Job Configuration
	 */
	@ObjectClassDefinition(name = "EMEA CSV Generation Scheduler Job for Integration with IICS and P360")
	public @interface Config {

		@AttributeDefinition(name = "Disabled")
		boolean disabled() default true;

		@AttributeDefinition(name = "Scheduler expression", defaultValue = "0 0 9 1/1 * ? *")
		String scheduler_expression();

		@AttributeDefinition(name = "Languages Generic List Path", description = "Path of Generic List containing the list of EMEA KA Languages")
		String languages_generic_list_path() default EMEAConstants.LANGUAGES_GENERIC_LIST_PATH;

		@AttributeDefinition(name = "Number of Days for Purge", description = "Integer Value of number of days after which the older CSVs will be deleted")
        int number_of_days_purge() default 30;

		@AttributeDefinition(name = "Asset URL Mapping ", description = "URL Mapping for generating Assets URLs as defined in PimDataTransformerImpl.Config ")
		String asset_url_mapping() default EMEAConstants.DEFAULT_URL_MAPPING;

		@AttributeDefinition(name = "Paths to be Ignored", description = "Paths under /content/dam/emea that are to be ignored from the generated CSV files")
		String[] denied_paths() default { EMEAConstants.DAM_PIM_FOLDER };
	}
}
