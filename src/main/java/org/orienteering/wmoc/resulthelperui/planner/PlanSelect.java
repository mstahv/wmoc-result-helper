package org.orienteering.wmoc.resulthelperui.planner;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.orienteering.wmoc.domain.planner.AllPlans;
import org.orienteering.wmoc.domain.planner.StartTimePlan;
import org.vaadin.firitin.components.progressbar.VProgressBar;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SpringComponent
@UIScope
public class PlanSelect extends Select<StartTimePlan> {

    private final PlannerService service;

    public PlanSelect(PlannerService plannerService) {
        this.service = plannerService;
        setLabel("Start time plan:");
        setItemLabelGenerator(p -> p.getPlanName());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Future<AllPlans> allPlans = service.getAllPlans();
        add(VProgressBar.indeterminateForTask(() -> {
            try {
                setItems(allPlans.get().getPlans());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
