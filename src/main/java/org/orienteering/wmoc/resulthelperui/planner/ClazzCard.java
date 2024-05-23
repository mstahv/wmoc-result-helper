package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.textfield.VIntegerField;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.components.timepicker.VTimePicker;

import java.time.LocalTime;
import java.util.Arrays;

public class ClazzCard extends Div {
    private final Clazz c;
    private TimePicker firstStart = new VTimePicker()
            .withWidth("5.5em");
    private VTextField name = new VTextField().withWidth("5em");
    private IntegerField startInterval = new VIntegerField()
            .withLabel("Start interval")
            .withAutoselect(true);
    private IntegerField estimatedRunners = new VIntegerField()
            .withLabel("Reserved start slots")
            .withAutoselect(true);
    private HorizontalLayout mainArea = new VHorizontalLayout().alignAll(FlexComponent.Alignment.CENTER);
    private ClazzCard next;

    Binder<Clazz> binder = new BeanValidationBinder<>(Clazz.class);
    private Paragraph lastStartParagraph = new Paragraph();

    public ClazzCard(Clazz c) {
        this.c = c;
        init();
    }

    void init() {
        removeAll();
        mainArea.removeAll();
        add(mainArea);
        DragSource<HorizontalLayout> dragSource = DragSource.create(mainArea);
        dragSource.setDraggable(true);
        Icon dragHandle = VaadinIcon.MENU.create();
        dragHandle.setColor("gray");
        dragHandle.getStyle().setMarginRight("0.5em");
        dragHandle.setTooltipText("""
            Dragging on top of another class moves the class (and next classes in the queue) to start after it.
            Dragging over the "start area" creates a new queue from the current class (and possible queued classes).
            Dragging queue root on top of itself DELETES the queue.
        """);

        DropTarget<HorizontalLayout> target = DropTarget.create(mainArea);
        target.addDropListener(e -> {
            e.getDragSourceComponent().ifPresent(source -> {
                ClazzCard draggedCard = source.findAncestor(ClazzCard.class);
                StartCard startCard = source.findAncestor(StartCard.class);
                boolean wasQueueRoot = draggedCard.getClazz().isQueueRoot();
                try {
                    draggedCard.c.setStartsAfter(c);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
                if(wasQueueRoot) {
                    startCard.getStart()
                            .getStartQueues().remove(draggedCard.getClazz());
                    startCard.init();
                } else {
                    ClazzCard cardAncestor = draggedCard.findAncestor(ClazzCard.class);
                    if(this != cardAncestor) {
                        cardAncestor.init();
                    }
                }

                init();
                Notification.show("Dropped on card " + c.getName());
                if(c.isQueueRoot() && draggedCard == this) {
                    Notification.show("Deleted");
                }
            });

        });

        mainArea.add(dragHandle, name);

        mainArea.add(firstStart);
        firstStart.setEnabled(c.isQueueRoot());

        Button otherDetails = new VButton(VaadinIcon.COG.create())
                .withThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE)
                .withTooltip("Class settings...")
                .onClick(() -> {
                    var d = new Dialog(
                            new VVerticalLayout(
                                    new H2("Class settings for " + c.getName()),
                                    startInterval,
                                    estimatedRunners,
                                    new Button("Done", e-> e.getSource().findAncestor(Dialog.class).close())
                            ).withFullWidth());
                    d.setWidth("70%");
                    d.open();
                });
        Button clone = new VButton(VaadinIcon.COPY.create())
                .withThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE)
                .withTooltip("Clone the class to the same queue")
                .onClick(() -> {
            Clazz clazz = new Clazz(getClazz().getName() + " (copy)", getClazz().getStart());
            clazz.setEstimatedRunners(getClazz().getEstimatedRunners());
            clazz.setStartInterval(getClazz().getStartInterval());
            clazz.setStartsAfter(getClazz());
            StartCard startCard = findAncestor(StartCard.class);
            startCard.init();
            startCard.focus(clazz);
        });

        mainArea.add(otherDetails, clone);
        binder.bindInstanceFields(this);
        binder.setBean(c);
        if(c.isQueueRoot()) {
            firstStart.addValueChangeListener(e -> updateStartTimes());
        }
        Arrays.asList(startInterval, estimatedRunners).forEach(
                c -> c.addValueChangeListener(e -> updateStartTimes()));

        addChild();

        if(c.isQueueRoot()) {
            this.lastStartParagraph = new Paragraph();
            add(lastStartParagraph);
            updateStartTimes();
        }
    }

    public void updateStartTimes() {
        if(getClazz().isQueueRoot()) {
            LocalTime lastStart = c.getFirstStart();
            lastStart = lastStart.plusSeconds(c.getEstimatedRunners()* c.getStartInterval());

            if(c.getNextClazz() != null) {
                Clazz current = c;
                while (current.getNextClazz() != null) {
                    current = current.getNextClazz();
                    lastStart = lastStart.plusSeconds(current.getEstimatedRunners() * current.getStartInterval());
                }
            }
            lastStartParagraph.setText("Last in of queue: " + lastStart);
        } else {
            // force re-reading the calculated firstStartTime
            binder.refreshFields();
        }
        if(next != null) {
            next.updateStartTimes();
        }
    }

    private void addChild() {
        Clazz n = c.getNextClazz();
        if(n != null) {
            next = new ClazzCard(n);
            add(next);
        }
    }

    public Clazz getClazz() {
        return c;
    }

    public void focus() {
        name.selectAll();
    }
}
