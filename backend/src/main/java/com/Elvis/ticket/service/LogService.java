package com.Elvis.ticket.service;

import com.Elvis.ticket.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    
    // Pattern to match log entries in the format from your log file:
    // 2025-05-18 15:22:25 INFO  o.h.validator.internal.util.Version - HV000001: Hibernate Validator 8.0.1.Final
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+" + // timestamp
            "(INFO|DEBUG|WARN|ERROR|TRACE)\\s+" +              // log level
            "([\\w\\.\\$]+)\\s+-\\s+" +                        // logger name
            "(.*)");                                           // message

    // Change this to match your actual log directory
    @Value("${logging.file.path:log}")
    private String logDirectory;

    @PostConstruct
    public void init() {
        logger.info("LogService initialized");
        logger.info("Current working directory: {}", System.getProperty("user.dir"));
        logger.info("Configured log directory: {}", logDirectory);
        
        // Check if log directory exists
        File dir = new File(logDirectory);
        logger.info("Log directory absolute path: {}", dir.getAbsolutePath());
        logger.info("Log directory exists: {}", dir.exists());
        
        if (dir.exists()) {
            // List files in log directory
            File[] files = dir.listFiles();
            if (files != null) {
                logger.info("Files in log directory:");
                for (File file : files) {
                    logger.info(" - {} ({})", file.getName(), file.length());
                }
            } else {
                logger.warn("No files found in log directory or listFiles() returned null");
            }
        } else {
            logger.warn("Log directory does not exist: {}", dir.getAbsolutePath());
        }
    }

    /**
     * Get a list of all available log files
     */
    public List<String> getLogFiles() {
        try {
            File dir = new File(logDirectory);
            String absolutePath = dir.getAbsolutePath();
            logger.info("Looking for log files in: {}", absolutePath);
            
            // Check if directory exists
            if (!dir.exists()) {
                logger.error("Log directory does not exist: {}", absolutePath);
                return Collections.emptyList();
            }
            
            // Check if it's a directory
            if (!dir.isDirectory()) {
                logger.error("Path is not a directory: {}", absolutePath);
                return Collections.emptyList();
            }
            
            // List all files in the directory
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                logger.info("No files found in directory: {}", absolutePath);
                return Collections.emptyList();
            }
            
            List<String> fileNames = Arrays.stream(files)
                    .filter(File::isFile)
                    .map(File::getName)
                    .filter(name -> name.endsWith(".log")) // Only include .log files
                    .collect(Collectors.toList());
            
            logger.info("Found {} log files: {}", fileNames.size(), fileNames);
            return fileNames;
        } catch (Exception e) {
            logger.error("Error listing log files", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get log entries from a specific file with optional filtering
     */
    public List<LogEntry> getLogContent(String fileName, String level, String search, Integer limit) {
        if (fileName == null || fileName.isBlank()) {
            return Collections.emptyList();
        }
        final Path resolvedPath;
        try {
            String safeFileName = Paths.get(fileName).getFileName().toString();
            if (!safeFileName.endsWith(".log")) {
                return Collections.emptyList();
            }
            Path basePath = Paths.get(logDirectory).toAbsolutePath().normalize();
            resolvedPath = basePath.resolve(safeFileName).normalize();
            if (!resolvedPath.startsWith(basePath)) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        File logFile = resolvedPath.toFile();
        logger.info("Attempting to read log file: {}", logFile.getAbsolutePath());
        logger.info("File exists: {}, size: {} bytes", logFile.exists(), logFile.length());
        
        // Check if file exists
        if (!logFile.exists()) {
            logger.error("Log file does not exist: {}", logFile.getAbsolutePath());
            return Collections.emptyList();
        }
        
        // Check if it's a file
        if (!logFile.isFile()) {
            logger.error("Path is not a file: {}", logFile.getAbsolutePath());
            return Collections.emptyList();
        }
        
        List<LogEntry> entries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int count = 0;
            
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        // Parse the log entry
                        String timestampStr = matcher.group(1);
                        String logLevel = matcher.group(2);
                        String loggerName = matcher.group(3);
                        String message = matcher.group(4);
                        
                        LocalDateTime timestamp = LocalDateTime.parse(timestampStr, formatter);
                        
                        LogEntry entry = new LogEntry();
                        entry.setTimestamp(timestamp);
                        entry.setLevel(logLevel);
                        entry.setLogger(loggerName);
                        entry.setThreadName(""); // Thread name not available in this log format
                        entry.setMessage(message);
                        
                        // Apply filters
                        if (shouldIncludeEntry(entry, level, search)) {
                            entries.add(entry);
                            count++;
                            
                            // Check if we've reached the limit
                            if (limit != null && count >= limit) {
                                break;
                            }
                        }
                    } catch (DateTimeParseException e) {
                        logger.warn("Failed to parse timestamp: {}", matcher.group(1), e);
                    }
                } else {
                    // Line doesn't match our pattern, try to append to the previous entry if possible
                    if (!entries.isEmpty()) {
                        LogEntry lastEntry = entries.get(entries.size() - 1);
                        lastEntry.setMessage(lastEntry.getMessage() + "\n" + line);
                    }
                }
            }
            
            logger.info("Parsed {} log entries from file {}", entries.size(), fileName);
            return entries;
            
        } catch (IOException e) {
            logger.error("Error reading log file: {}", fileName, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if a log entry should be included based on filters
     */
    private boolean shouldIncludeEntry(LogEntry entry, String level, String search) {
        // Filter by level if specified
        if (level != null && !level.isEmpty() && !level.equalsIgnoreCase("all")) {
            if (!entry.getLevel().equalsIgnoreCase(level)) {
                return false;
            }
        }
        
        // Filter by search term if specified
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            return entry.getMessage().toLowerCase().contains(searchLower) ||
                   entry.getLogger().toLowerCase().contains(searchLower);
        }
        
        return true;
    }
}
