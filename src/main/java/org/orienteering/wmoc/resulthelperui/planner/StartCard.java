package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;

import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StartCard extends VerticalLayout implements DropTarget<StartCard> {
    private final Start start;

    public StartCard(Start start) {
        this.start = start;
        init();

        setDropEffect(DropEffect.MOVE);
        setActive(true);
        addDropListener(e -> {
            ClazzCard component = e.getDragSourceComponent().get().findAncestor(ClazzCard.class);
            Clazz clazz = component.getClazz();
            StartCard ancestor = component.findAncestor(StartCard.class);
            if (clazz.isQueueRoot()) {
                if (start.getStartQueues().contains(clazz)) {
                    Notification.show("Already a first class of the queue in this start");
                    return;
                }
                // remove from current start
                ancestor.getStart().getStartQueues().remove(clazz);
                getStart().getStartQueues().add(clazz);
            } else {
                LocalTime calculatedCurrentFirstStart = clazz.getFirstStart();
                clazz.setFirstStart(calculatedCurrentFirstStart);
            }

            ancestor.init();
            if (ancestor != this) {
                init();
            }

            Notification.show("Dropped on Start!");
        });
    }

    void init() {
        removeAll();
        setPadding(true);
        setSpacing(false);
        add(new H2(start.getName()));
        HorizontalLayout startqueues = new VHorizontalLayout()
                .withAlignItems(Alignment.START);

        add(new VButton(VaadinIcon.FILE_ADD.create())
                .withThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE)
                .withTooltip("Add new class & queue to this start")
                .onClick(() -> {
                    Clazz clazz = new Clazz("New class", start);
                    clazz.setFirstStart(LocalTime.of(11,0));
                    VerticalLayout queue = new VerticalLayout();
                    ClazzCard clazzCard = new ClazzCard(clazz);
                    queue.add(clazzCard);
                    startqueues.add(queue);
                    clazzCard.focus();
                }));

        add(startqueues);
        Collection<Clazz> clazzes = start.getStartQueues();
        for (Clazz c : clazzes) {
            VerticalLayout queue = new VerticalLayout();
            queue.add(new ClazzCard(c));
            startqueues.add(queue);
        }
    }

    public Start getStart() {
        return start;
    }

    private static Stream<Component> hier(Component c) {
        return Stream.concat(Stream.of(c), c.getChildren().flatMap(c2 -> hier(c2)));
    }

    public void focus(Clazz clazz) {
        hier(this).filter(c -> {
            if(c instanceof ClazzCard cc) {
                return cc.getClazz() == clazz;
            }
            return false;
        }).map(c -> (ClazzCard) c).findFirst().ifPresent(c -> c.focus());
    }
}
