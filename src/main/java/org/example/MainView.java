package org.example;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.firitin.components.RichText;

@Route(layout = TopLayout.class)
public class MainView extends VerticalLayout {

    public MainView() {
        add(new RichText().withMarkDown("""
        This app contains various helpers for WMOC result service.
        
        Source code (Open Source): https://github.com/mstahv/wmoc-result-helper
        
        """));
    }
}
