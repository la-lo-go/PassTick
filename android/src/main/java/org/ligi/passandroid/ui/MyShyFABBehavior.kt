package org.ligi.passandroid.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu
import org.ligi.passandroid.R

class MyShyFABBehavior : CoordinatorLayout.Behavior<FloatingActionsMenu> {
    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionsMenu, dependency: View) =
        dependency is AppBarLayout

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionsMenu,
        dependency: View,
    ): Boolean {
        if (dependency is AppBarLayout) {
            val bottomMargin = (child.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
            val distance = if (child.isExpanded) {
                child.height + bottomMargin
            } else {
                child.resources.getDimension(R.dimen.fab_size_normal).toInt() + 2 * bottomMargin
            }
            child.translationY = -distance * dependency.y / toolbarHeight(dependency.context)
        }
        return false
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionsMenu, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
        onDependentViewChanged(parent, child, dependency)
    }

    private fun toolbarHeight(context: Context): Float {
        val attributes = context.theme.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
        return try {
            attributes.getDimension(0, 1f)
        } finally {
            attributes.recycle()
        }
    }
}
