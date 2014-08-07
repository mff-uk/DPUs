package cz.cuni.mff.xrg.uv.boost.dpu.addon;

import cz.cuni.mff.xrg.uv.boost.dpu.advanced.DpuAdvancedBase;

/**
 * <strong>If addon use configuration then the configuration class
 * must be static!</strong>
 *
 * @author Škoda Petr
 */
public interface Addon {

    /**
     *
     * @param context
     * @return False if DPU's user code should not be executed.
     */
    boolean preAction(DpuAdvancedBase.Context context);

    /**
     * Is executed after DPU's user code, or after all
     * {@link #preAction(cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext, cz.cuni.mff.xrg.uv.boost.dpu.config.ConfigManager)}
     * if some of them return true.
     *
     * @param context
     */
    void postAction(DpuAdvancedBase.Context context);

}
