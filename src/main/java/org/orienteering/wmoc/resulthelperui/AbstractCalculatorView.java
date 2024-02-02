package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(layout = TopLayout.class)
abstract class AbstractCalculatorView extends VerticalLayout {

    private List<String> errors =new ArrayList<>();
    void collectPossibleError(String s) {
        errors.add(s);
    }

    void notifyErrors() {
        errors.forEach(s ->  Notification.show(s, 5000, Notification.Position.MIDDLE));
        errors.clear();
    }

    List<String> getErrors() {
        return errors;
    }

}
