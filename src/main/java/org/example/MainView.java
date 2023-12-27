package org.example;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.RichText;

@Route(layout = TopLayout.class)
@MenuItem(title = "About", order = MenuItem.BEGINNING)
public class MainView extends VerticalLayout {

    public MainView() {
        add(new RichText().withMarkDown("""
        # WMOC result service helper
        
        This app contains various helpers for WMOC result service.
        
        [Source code available in GitHub (Open Source)](https://github.com/mstahv/wmoc-result-helper).
        
        """));
    }
}
