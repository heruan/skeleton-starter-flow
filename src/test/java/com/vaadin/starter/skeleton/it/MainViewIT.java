package com.vaadin.starter.skeleton.it;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;

public class MainViewIT extends VaadinIT {

    @Test
    public void clickingButtonShowsNotification() {
        open();

        Assert.assertFalse($(NotificationElement.class).exists());

        $(ButtonElement.class).first().click();

        Assert.assertTrue($(NotificationElement.class).waitForFirst().isOpen());
    }
}
