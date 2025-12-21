package org.example.audio_ecommerce.util;

import java.util.regex.Pattern;

public class MessageFilterUtil {
    
    // Regex patterns for detecting sensitive content
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
        "(\\+84|84|0)(3|5|7|8|9)[0-9]{8}\\b|" +  // Vietnamese mobile numbers
        "(\\+84|84|0)[0-9]{9,10}\\b"             // Other Vietnamese numbers
    );
    
    // Patterns for social media links
    private static final Pattern FACEBOOK_PATTERN = Pattern.compile(
        "\\b(facebook\\.com|fb\\.com|fb\\.me|m\\.me|facebook|fb)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ZALO_PATTERN = Pattern.compile(
        "\\b(zalo\\.me|zaloapp\\.com|zalo)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
        "\\b(instagram\\.com|instagr\\.am|ig\\.me|instagram)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TWITTER_PATTERN = Pattern.compile(
        "\\b(twitter\\.com|x\\.com|t\\.co|twitter)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TIKTOK_PATTERN = Pattern.compile(
        "\\b(tiktok\\.com|tiktok)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
        "\\b(youtube\\.com|youtu\\.be|youtube)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
        "\\b(linkedin\\.com|lnkd\\.in|linkedin)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TELEGRAM_PATTERN = Pattern.compile(
        "\\b(telegram\\.org|t\\.me|telegram)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern WEBSITE_PATTERN = Pattern.compile(
        "\\b(https?://)?(www\\.)?[a-zA-Z0-9][-a-zA-Z0-9]*\\.([a-zA-Z]{2,})(/[\\w\\-./?%&=]*)?\\b"
    );
    
    /**
     * Checks if a message contains sensitive content (phone numbers or social media links)
     * @param content The message content to check
     * @return true if sensitive content is detected, false otherwise
     */
    public static boolean containsSensitiveContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // Check for phone numbers
        if (PHONE_NUMBER_PATTERN.matcher(content).find()) {
            return true;
        }
        
        // Check for social media platforms
        if (FACEBOOK_PATTERN.matcher(content).find() ||
            ZALO_PATTERN.matcher(content).find() ||
            INSTAGRAM_PATTERN.matcher(content).find() ||
            TWITTER_PATTERN.matcher(content).find() ||
            TIKTOK_PATTERN.matcher(content).find() ||
            YOUTUBE_PATTERN.matcher(content).find() ||
            LINKEDIN_PATTERN.matcher(content).find() ||
            TELEGRAM_PATTERN.matcher(content).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Filters out sensitive content from a message by replacing it with a placeholder
     * @param content The message content to filter
     * @return The filtered message content
     */
    public static String filterSensitiveContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String filteredContent = content;
        
        // Replace phone numbers
        filteredContent = PHONE_NUMBER_PATTERN.matcher(filteredContent).replaceAll("[SỐ ĐIỆN THOẠI ĐÃ BỊ ẨN]");
        
        // Replace social media links/platforms
        filteredContent = FACEBOOK_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT FACEBOOK ĐÃ BỊ ẨN]");
        filteredContent = ZALO_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT ZALO ĐÃ BỊ ẨN]");
        filteredContent = INSTAGRAM_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT INSTAGRAM ĐÃ BỊ ẨN]");
        filteredContent = TWITTER_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT TWITTER ĐÃ BỊ ẨN]");
        filteredContent = TIKTOK_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT TIKTOK ĐÃ BỊ ẨN]");
        filteredContent = YOUTUBE_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT YOUTUBE ĐÃ BỊ ẨN]");
        filteredContent = LINKEDIN_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT LINKEDIN ĐÃ BỊ ẨN]");
        filteredContent = TELEGRAM_PATTERN.matcher(filteredContent).replaceAll("[LIÊN KẾT TELEGRAM ĐÃ BỊ ẨN]");
        
        return filteredContent;
    }
    
    /**
     * Validates a message and throws an exception if it contains sensitive content
     * @param content The message content to validate
     * @throws IllegalArgumentException if sensitive content is detected
     */
    public static void validateMessageContent(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        
        if (containsSensitiveContent(content)) {
            throw new IllegalArgumentException("Tin nhắn này chứa nội dung nhạy cảm (số điện thoại hoặc liên kết mạng xã hội) và không thể gửi đi.");
        }
    }
}