package com.vaadin.starter.skeleton.it;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.testbench.ScreenshotOnFailureRule;

public class MainViewIT extends VaadinIT {
    @Rule
    public ScreenshotOnFailureRule rule = new ScreenshotOnFailureRule(this,
            true);

    @Test
    public void clickingButtonShowsNotification() {
        open();

        Assert.assertFalse($(NotificationElement.class).exists());

        $(ButtonElement.class).first().click();

        Assert.assertTrue($(NotificationElement.class).waitForFirst().isOpen());
    }
}
