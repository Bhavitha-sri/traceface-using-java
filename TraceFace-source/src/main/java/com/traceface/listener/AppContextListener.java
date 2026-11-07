package com.traceface.listener;

import com.traceface.config.AppConfig;
import com.traceface.service.BlockchainService;
import com.traceface.service.EvidenceService;
import com.traceface.service.FaceService;
import com.traceface.service.ImageDownloader;
import com.traceface.service.WebFaceMatcher;
import com.traceface.service.WebSearchService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads OpenCV + AI models once and shares the services with all servlets. */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        try {
            Files.createDirectories(AppConfig.uploadDir());
            Files.createDirectories(AppConfig.dataDir());

            nu.pattern.OpenCV.loadLocally();            // native OpenCV library

            FaceService faceService = new FaceService(
                    AppConfig.detectorModel().toString(),
                    AppConfig.recognizerModel().toString());

            context.setAttribute("faceService", faceService);
            context.setAttribute("webSearchService", new WebSearchService());
            context.setAttribute("webFaceMatcher", new WebFaceMatcher(faceService, new ImageDownloader()));
            context.setAttribute("evidenceService", new EvidenceService());
            context.setAttribute("blockchainService", new BlockchainService());

            LOG.info("TraceFace started. Home directory: " + AppConfig.home());
        } catch (Exception | Error e) {
            LOG.log(Level.SEVERE, "TraceFace failed to start", e);
            throw new IllegalStateException("TraceFace failed to start: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOG.info("TraceFace stopped.");
    }
}
