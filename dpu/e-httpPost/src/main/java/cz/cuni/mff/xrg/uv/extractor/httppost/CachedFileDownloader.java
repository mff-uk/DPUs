package cz.cuni.mff.xrg.uv.extractor.httppost;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unifiedviews.helpers.dpu.extension.ExtensionException;
import eu.unifiedviews.helpers.dpu.config.ConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractExtensionDialog;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.Configurable;
import eu.unifiedviews.dpu.config.DPUConfigException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.net.ssl.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.helpers.dpu.exec.ExecContext;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.Context;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dpu.extension.Extension;

/**
 * Main functionality:
 * <ul>
 * <li>Download files from given URL</li>
 * <li>Contains optional simple file cache</li>
 * <li>User can specify the pause between downloads</li>
 * </ul>
 *
 * @see cz.cuni.mff.xrg.uv.boost.dpu.addonAddon
 * @author Škoda Petr
 */
public class CachedFileDownloader implements Extension, Extension.Executable,
        Configurable<CachedFileDownloader.Configuration_V1> {

    public static final String USED_USER_DIRECTORY = "addon/cachedFileDownloader";

    public static final String CACHE_FILE = "cacheContent.xml";

    public static final String USED_CONFIG_NAME = "addon/cachedFileDownloader";

    public static final String ADDON_NAME = "Custom Cached file downloader";

    private static final Logger LOG = LoggerFactory.getLogger(CachedFileDownloader.class);

    /**
     * Configuration class.
     */
    public static class Configuration_V1 {

        /**
         * Max number of attempts to download single file.
         */
        private Integer maxAttemps = 1;

        /**
         * Min pause between download of two files.
         */
        private Integer minPause = 3000;

        /**
         * Max pause between download of two files.
         */
        private Integer maxPause = 15000;

        /**
         * If true then data are always downloaded and existing data in caches are rewritten.
         */
        private boolean rewriteCache = true;

        private int maxDownloads = -1;

        public Configuration_V1() {
        }

        public Integer getMaxAttemps() {
            return maxAttemps;
        }

        public void setMaxAttemps(Integer maxAttemps) {
            this.maxAttemps = maxAttemps;
        }

        public Integer getMinPause() {
            return minPause;
        }

        public void setMinPause(Integer minPause) {
            this.minPause = minPause;
        }

        public Integer getMaxPause() {
            return maxPause;
        }

        public void setMaxPause(Integer maxPause) {
            this.maxPause = maxPause;
        }

        public Boolean isRewriteCache() {
            return rewriteCache;
        }

        public void setRewriteCache(Boolean rewriteCache) {
            this.rewriteCache = rewriteCache;
        }

        public int getMaxDownloads() {
            return maxDownloads;
        }

        public void setMaxDownloads(int maxDownloads) {
            this.maxDownloads = maxDownloads;
        }

    }

    /**
     * Vaadin configuration dialog.
     */
    public class VaadinDialog extends AbstractExtensionDialog<Configuration_V1> {

        private TextField txtMaxAttemps;

        private TextField txtMaxPause;

        private TextField txtMinPause;

        private CheckBox checkRewriteCache;

        private TextField txtMaxDownloads;

        public VaadinDialog() {
            super(configHistory);
        }

        @Override
        protected String getConfigClassName() {
            return USED_CONFIG_NAME;
        }

        @Override
        public void buildLayout() {
            final VerticalLayout mainLayout = new VerticalLayout();
            mainLayout.setSizeFull();
            mainLayout.setSpacing(true);
            mainLayout.setMargin(true);

            txtMaxAttemps = new TextField(
                    "Max number of attemps to download a single file, use -1 for infinity");
            txtMaxAttemps.setDescription("Set to 0 to use only files from cache");
            txtMaxAttemps.setWidth("5em");
            txtMaxAttemps.setRequired(true);
            mainLayout.addComponent(txtMaxAttemps);

            txtMaxPause = new TextField("Max pause in ms between downloads");
            txtMaxPause.setWidth("10em");
            txtMaxPause.setRequired(true);
            mainLayout.addComponent(txtMaxPause);

            txtMinPause = new TextField("Min pause in ms between downloads");
            txtMinPause.setWidth("10em");
            txtMinPause.setRequired(true);
            mainLayout.addComponent(txtMinPause);

            checkRewriteCache = new CheckBox("Rewrite cache");
            checkRewriteCache.setDescription(
                    "If checked then files are always downloaded and existing files in caches are rewritten.");
            mainLayout.addComponent(checkRewriteCache);

            txtMaxDownloads = new TextField("Max number of files to download.");
            txtMaxDownloads.setDescription("Use -1 for no limit.");
            txtMaxDownloads.setWidth("5em");
            txtMaxDownloads.setRequired(true);
            mainLayout.addComponent(txtMaxDownloads);

            setCompositionRoot(mainLayout);
        }

        @Override
        protected void setConfiguration(Configuration_V1 c) throws DPUConfigException {
            txtMaxAttemps.setValue(c.getMaxAttemps().toString());
            txtMaxPause.setValue(c.getMaxPause().toString());
            txtMinPause.setValue(c.getMinPause().toString());
            checkRewriteCache.setValue(c.isRewriteCache());
            txtMaxDownloads.setValue(Integer.toString(c.getMaxDownloads()));
        }

        @Override
        protected Configuration_V1 getConfiguration() throws DPUConfigException {
            if (!txtMaxAttemps.isValid() || !txtMaxPause.isValid() || !txtMinPause.isValid()) {
                throw new DPUConfigException("All values for " + ADDON_NAME + " must be provided.");
            }

            final Configuration_V1 c = new Configuration_V1();

            try {
                c.setMaxAttemps(Integer.parseInt(txtMaxAttemps.getValue()));
                c.setMaxPause(Integer.parseInt(txtMaxPause.getValue()));
                c.setMinPause(Integer.parseInt(txtMinPause.getValue()));
                c.setMaxDownloads(Integer.parseInt(txtMaxDownloads.getValue()));
            } catch (NumberFormatException ex) {
                throw new ConfigException("Provided valuas must be numbers.", ex);
            }

            if (c.getMaxPause() < c.getMinPause()) {
                throw new ConfigException("Max pause must be greater then min pause.");
            }

            c.setRewriteCache(checkRewriteCache.getValue());
            return c;
        }

    }

    /**
     * Used configuration.
     */
    private Configuration_V1 config = new Configuration_V1();

    private final ConfigHistory<Configuration_V1> configHistory = ConfigHistory.noHistory(
            Configuration_V1.class);

    /**
     * Time of next download. Used to create randomly distributes pauses between downloads. Should be ignored
     * if cache is used.
     */
    private long nextDownload = new Date().getTime();

    /**
     * Base directory where store files.
     */
    private File baseDirectory = null;

    /**
     * DPU's master context.
     */
    private DPUContext dpuContext;

    private Context context;

    private int numberOfDownloads = 0;

    public CachedFileDownloader() {
    }

    @Override
    public void preInit(String param) throws DPUException {
        // No-op.
    }

    @Override
    public void afterInit(Context context) {
        this.context = context;
        if (context instanceof ExecContext) {
            this.dpuContext = ((ExecContext) context).getDpuContext();
        }
    }

    @Override
    public void execute(ExecutionPoint execPoint) throws ExtensionException {
        // File with store cache content.
        this.baseDirectory = new File(new File(java.net.URI.create(
                dpuContext.getDpuInstanceDirectory())), USED_USER_DIRECTORY);
        this.baseDirectory.mkdirs();

        if (execPoint == ExecutionPoint.POST_EXECUTE) {
            ContextUtils.sendShortInfo(context.asUserContext(), "Number of downloaded files: {0}",
                    numberOfDownloads);
        }

        if (execPoint != ExecutionPoint.PRE_EXECUTE) {
            return;
        }

        // Prepare cache directory.
        try {
            // Load configuration.
            this.config = context.getConfigManager().get(USED_CONFIG_NAME, configHistory);
        } catch (ConfigException ex) {
            ContextUtils.sendWarn(context.asUserContext(), "Addon failed to load configuration", ex,
                    "Failed to load configuration for: {0} default configuration is used.", ADDON_NAME);
            this.config = new Configuration_V1();
        }

        if (this.config == null) {
            ContextUtils.sendWarn(context.asUserContext(), "Addon failed to load configuration",
                    "Failed to load configuration for: {0} default configuration is used.", ADDON_NAME);
            this.config = new Configuration_V1();
        }
        LOG.info("BaseDirectory: {}", baseDirectory);

        // Ignore all certificates, added because of MICR_3 pipeline.
        // TODO: should be more focused on current job, not generaly remove the ssh check!
        try {
            setTrustAllCerts();
        } catch (Exception ex) {
            throw new ExtensionException("setTrustAllCerts throws", ex);
        }
    }

    @Override
    public Class<CachedFileDownloader.Configuration_V1> getConfigClass() {
        return CachedFileDownloader.Configuration_V1.class;
    }

    @Override
    public String getDialogCaption() {
        return ADDON_NAME;
    }

    @Override
    public AbstractExtensionDialog<Configuration_V1> getDialog() {
        return new VaadinDialog();
    }

    /**
     * If file of given name exists, then it's returned. If not then is downloaded from given URL, saved under
     * given name and then returned.
     *
     * @param fileName   Must not be null. Unique identification for the given file.
     * @param postMethod
     * @return Null if max number to download exceed given limit.
     * @throws ExtensionException
     * @throws IOException
     */
    public File post(String fileName, PostMethod postMethod) throws ExtensionException, IOException, DPUException {
        if (baseDirectory == null) {
            throw new ExtensionException("Not initialized!");
        }
        // Made name secure, so we can use it as a file name.
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Hard coded encoding is not supported!!", ex);
        }
        // Get file name.
        final File file = new File(baseDirectory, fileName);

        // Check cache.
        if (file.exists() && !config.rewriteCache) {
            LOG.debug("cache - {} ", fileName);
            return file;
        }

        // Check if we should download file.
        if (config.maxAttemps == 0) {
            LOG.info("No file found for: {}, {}", fileName);
            return null;
        }

        // Also return null if we exceeded number of downloads.
        if (config.maxDownloads != -1 && numberOfDownloads > config.maxDownloads) {
            LOG.info("Number of downloads exceeded!");
            return null;
        }

        LOG.debug("post - {} ", fileName);
        // Download file with some level of fault tolerance.
        int attempCounter = config.maxAttemps;
        while (attempCounter != 0 && !dpuContext.canceled()) {
            // Wait before download.
            waitForNextDownload();
            // Try to download file.
            try {
                final HttpClient client = new HttpClient();
                int returnCode = client.executeMethod(postMethod);
                if (returnCode != HttpStatus.SC_OK) {
                    throw ContextUtils.dpuException(context.asUserContext(), "Response code: {0}",
                            returnCode);
                }
                // Copy content.
                FileUtils.copyInputStreamToFile(postMethod.getResponseBodyAsStream(), file);
                ++numberOfDownloads;
                return file;
            } catch (IOException ex) {
                LOG.warn("Failed to download file from {} attemp {}/{}", fileName, attempCounter,
                        config.maxAttemps, ex);
            }
            // Decrease attemp counted if not set to infinity = -1.
            if (attempCounter > 0) {
                --attempCounter;
            }
        }
        // If we are here we does not manage to download file. So check for the reason.
        if (dpuContext.canceled()) {
            // Execution has been canceled.
            throw ContextUtils.dpuExceptionCancelled(context.asUserContext());
        } else {
            // We were unable to download file in given number of attemps, we have faild.
            throw new IOException("Can't obtain file named: '" + fileName + "'");
        }
    }

    public int getNumberOfDownloads() {
        return numberOfDownloads;
    }

    /**
     * Wait before next download. Before leaving set time for next download.
     */
    private void waitForNextDownload() {
        while ((new Date()).getTime() < nextDownload && !dpuContext.canceled()) {
            try {
                Thread.sleep(700);
            } catch (InterruptedException ex) {

            }
        }
        // Determine min time for next download, ie. time when next file can be downloaded.
        nextDownload = new Date().getTime()
                + (long) (Math.random() * (config.maxPause - config.minPause) + config.minPause);
    }

    /**
     * We will trust all certificates!
     *
     * Code source is MICR_3 DPU. TODO: Do not trust all certificates globally.
     *
     * @throws Exception
     */
    public static void setTrustAllCerts() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager.
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String urlHostName, SSLSession session) {
                            return true;
                        }
                    });
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            LOG.error("Can't install all-trusting trus manager", ex);
        }
    }

}
