package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.orienteering.wmoc.domain.planner.AllPlans;
import org.orienteering.wmoc.domain.planner.Clazz;
import org.orienteering.wmoc.domain.planner.Start;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.orienteering.wmoc.resulthelperui.TopLayout;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.progressbar.VProgressBar;
import org.vaadin.firitin.components.upload.UploadFileHandler;
import org.vaadin.firitin.util.BrowserPrompt;
import org.vaadin.firitin.util.VStyleUtil;
import org.vaadin.firitin.util.style.Padding;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@UIScope
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
                DynamicFileDownloader downloadCsv = new DynamicFileDownloader(
                        new VButton(VaadinIcon.GRID.create())
                                .withTooltip("Downloads a CSV file of the selected plan"),
                        "plan.csv", outputStream -> {
                        plannerService.toCsv(plans.getValue(), outputStream);
                });

                UploadFileHandler ufh = new UploadFileHandler((is, md) -> {
                    try {
                       var plan = plannerService.readBackup(is.readAllBytes());
                       return () -> {
                        plans.getGenericDataView().refreshAll();
                        plans.setValue(plan);
                       };
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).withDragAndDrop(false)
                        .withUploadButton(new VButton(VaadinIcon.UPLOAD.create())
                        .withTooltip("Upload plan file (.dat)"));
                UploadFileHandler uploadCsv = new UploadFileHandler((is, md) -> {
                    try {
                        var plan = plannerService.readCsv(is, md.fileName());
                        return () -> {
                            plans.getGenericDataView().refreshAll();
                            plans.setValue(plan);
                        };
                    } catch (Exception ex) {
                        return () -> {
                            Notification.show("Error reading CSV: " + ex.getMessage());
                        };
                    }
                }).withDragAndDrop(false)
                        .withUploadButton(new VButton(VaadinIcon.UPLOAD.create())
                                .withTooltip("Upload plan as CSV (check format from download)"));

                planSelector = new VHorizontalLayout()
                        .space()
                        .withComponents(plans, addPlan, save, backupDownload, downloadCsv, ufh, uploadCsv)
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

        Button setAllIntervals = new VButton("Set interval for all classes...", () -> {
            BrowserPrompt.promptInteger("New interval for all classes?")
                    .thenAccept(interval -> {
                        StartTimePlan plan = binder.getBean();
                        plan.setIntervalForAll(interval);
                        // rebuild view
                        edit(plan);
                    });
        });

        add(new VHorizontalLayout(planName, remove, setAllIntervals, new VButton("Validate & statistics...", () -> {
            showValidationAndStats();
        })));
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

    private void showValidationAndStats() {

        StartTimePlan plan = plans.getValue();

        int totalRunnersExpected = 0;
        int classCount = 0;
        Set<String> classNames = new TreeSet<>();
        Set<String> dupes = new TreeSet<>();
        List<Start> starts = plan.getStarts();
        for(Start s : starts) {
            Set<Clazz> startQueues = s.getStartQueues();
            for(Clazz c : startQueues) {
                while(c != null) {
                    classCount++;
                    if(classNames.contains(c.getName())) {
                        dupes.add(c.getName());
                    } else {
                        classNames.add(c.getName());
                    }
                    totalRunnersExpected = totalRunnersExpected + c.getEstimatedRunners();
                    c = c.getNextClazz();
                }
            }
        }

        String report = """
            Total runners estimated/expected: %s
        
            Classes: %s 
            %s
            
            Dupes:  %s (should be zero!) 
            %s
                
            """.formatted(
                totalRunnersExpected,
                classCount, onePerLine(classNames),
                dupes.size(), onePerLine(dupes)
        );

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Validation results:");
        dialog.add(new Pre(report));
        dialog.open();
    }

    private String onePerLine(Set<String> set) {
        return set.stream().collect(Collectors.joining("\n"));
    }

}
