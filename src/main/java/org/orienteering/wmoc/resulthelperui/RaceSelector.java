package org.orienteering.wmoc.resulthelperui;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.select.Select;
import org.orienteering.datastandard._3.EntryList;
import org.orienteering.datastandard._3.Race;

import java.util.function.Consumer;

public class RaceSelector extends Select<Race> {

    public RaceSelector(EntryList el) {
        setItemLabelGenerator(r -> r.getName());
        setItems(el.getEvent().getRace());
    }

    public static void promptRaceId(EntryList el, Consumer<Integer> callback) {
        RaceSelector selector = new RaceSelector(el);
        Dialog dialog = new Dialog(new H3("Choose race:"), selector);

        selector.addValueChangeListener(e -> {
            var raceId = e.getValue().getRaceNumber().intValue();
            dialog.close();
            callback.accept(raceId);
        });
        dialog.open();
    }

}
