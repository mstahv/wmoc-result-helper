package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import org.orienteering.wmoc.domain.planner.AllPlans;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.orienteering.wmoc.resulthelperui.TopLayout;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.progressbar.VProgressBar;
import org.vaadin.firitin.components.upload.VUpload;
import org.vaadin.firitin.util.VStyleUtil;
import org.vaadin.firitin.util.style.Padding;

import java.io.IOException;
import java.util.concurrent.Future;

@Route(layout = TopLayout.class)
public class StartTimePlannerView extends VerticalLayout {

    private final PlannerService plannerService;
    private Select<StartTimePlan> plans;
    private VHorizontalLayout planSelector;

    public StartTimePlannerView(PlannerService plannerService) {
        this.plannerService = plannerService;

        VStyleUtil.inject("""
            .v-drag-over-target {
                outline: 1px solid lightblue;
            }
        """);
        add("This view can be used to plan start queues");
        addToolbar();
    }

    private void addToolbar() {
        Future<AllPlans> allPlans = plannerService.getAllPlans();
        add(VProgressBar.indeterminateForTask(() -> {
            try {
                AllPlans startTimePlans = allPlans.get();
                plans = new Select<>();
                plans.setItemLabelGenerator(p -> p.getPlanName());
                plans.setItems(startTimePlans.getPlans());
                plans.addValueChangeListener(e -> {
                    if(e.getValue() != null)
                        edit(e.getValue());
                });
                if(!startTimePlans.getPlans().isEmpty()) {
                    plans.setValue(startTimePlans.getPlans().get(0));
                }
                Button addPlan = new Button(VaadinIcon.FILE.create());
                addPlan.setTooltipText("Create new plan");
                addPlan.addClickListener(e -> {
                    StartTimePlan startTimePlan = new StartTimePlan();
                    startTimePlan.setPlanName("New plan");
                    startTimePlans.getPlans().add(startTimePlan);
                    edit(startTimePlan);
                });
                Button save = new Button("Save", VaadinIcon.DATABASE.create());
                save.setTooltipText("Saves your plans to browsers local storage.");
                save.addClickListener(e -> {
                    plannerService.saveToLocalStorage();
                    Notification.show("Saved to local storage! Consider downloading backup file for important plans.");
                });

                DynamicFileDownloader backupDownload = new DynamicFileDownloader(
                        new VButton(VaadinIcon.DOWNLOAD.create())
                            .withTooltip("Downloads backup of the selected plan (.dat file, for sharing with others & important plans)."),
                        "plan.dat", outputStream -> {
                            byte[] backup = plannerService.backup(plans.getValue());
                            try {
                                outputStream.write(backup);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                });

                VUpload upload = new VUpload()
                        .withAutoUpload(true)
                        .withDropAllowed(false);
                MemoryBuffer buffer = new MemoryBuffer();
                upload.setReceiver(buffer);
                Button uploadButton = new Button(VaadinIcon.UPLOAD.create());
                uploadButton.setTooltipText("Upload plan file (.dat)");
                upload.setUploadButton(uploadButton);
                upload.addSucceededListener(event -> {
                    try {
                        byte[] bytes = buffer.getInputStream().readAllBytes();
                        StartTimePlan plan = plannerService.readBackup(bytes);
                        plans.getGenericDataView().refreshAll();
                        plans.setValue(plan);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                planSelector = new VHorizontalLayout()
                        .space()
                        .withComponents(plans, addPlan, save, backupDownload, upload)
                        .withPadding(Padding.Side.RIGHT)
                        .alignAll(Alignment.CENTER);
                findAncestor(TopLayout.class).addToNavbar(planSelector);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private TextField planName = new TextField();
    BeanValidationBinder<StartTimePlan> binder = new BeanValidationBinder<>(StartTimePlan.class);

    private void edit(StartTimePlan startTimePlan) {
        removeAll();

        Button remove = new VButton(VaadinIcon.TRASH.create())
                .withTooltip("Remove this plan")
                .withClickListener(e -> {
                    plannerService.remove(startTimePlan);
                    plans.getGenericDataView().refreshAll();
                    plans.setValue(plans.getGenericDataView().getItem(0));
                });

        add(new VHorizontalLayout(planName, remove));
        binder.bindInstanceFields(this);
        binder.setBean(startTimePlan);

        binder.addValueChangeListener(e -> {
            if(plans.getValue() != null) {
                plans.getListDataView().refreshItem(plans.getValue());
            }
        });

        for (Start start : startTimePlan.getStarts()) {
            StartCard card = new StartCard(start);
            add(card);
        }

        Button addStart = new Button("New start");
        addStart.addClickListener(e -> {
            int id = startTimePlan.getStarts().size() + 1;
            Start start = new Start("Start " + id);
            startTimePlan.getStarts().add(start);
            // full re-render of the screen, but shouldn't be an issue
            edit(startTimePlan);
        });
        add(addStart);

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(planSelector != null) {
            planSelector.removeFromParent(); // in main layout
        }
        super.onDetach(detachEvent);
    }
    
}
