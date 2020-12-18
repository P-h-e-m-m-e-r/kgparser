package ru.klavogonki.kgparser.freemarker;

import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
public class LoginToPageTemplate extends FreemarkerTemplate {

    private static final String LOGIN_TO_PAGE_KEY = "loginToPage";
    private static final String LOGIN_TO_PAGE_STRING_KEY = "loginToPageString";

    @Override
    public String getTemplatePath() {
        return "top-by-speed-login-to-page.ftl";
    }

    public LoginToPageTemplate loginToPage(Map<String, Integer> loginToPage) {
        templateData.put(LOGIN_TO_PAGE_KEY, loginToPage);
        return this;
    }

    public Map<String, Integer> getLoginToPage() {
        return (Map<String, Integer>) templateData.get(LOGIN_TO_PAGE_KEY);
    }

    public LoginToPageTemplate loginToPageString(String loginToPageString) {
        templateData.put(LOGIN_TO_PAGE_STRING_KEY, loginToPageString);
        return this;
    }

    public String getLoginToPageString() {
        return (String) templateData.get(LOGIN_TO_PAGE_STRING_KEY);
    }

    @Override
    public void export(final String filePath) {
        // todo: validate keys presence?
        super.export(filePath);

        logger.debug(
            "Saved login -> page mapping to file {}. Total logins: {}",
            filePath,
            getLoginToPage().size()
        );
    }
}
