package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.SessionTemplate

class TemplatesActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var adapter: TemplatesAdapter
    private var templates: MutableList<SessionTemplate> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_templates)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        templates = settings.sessionTemplates.toMutableList()

        adapter = TemplatesAdapter(
            onUseClick = { tpl -> useTemplate(tpl) },
            onMenuClick = { tpl, anchor -> showPopupMenu(tpl, anchor) }
        )

        val recycler = findViewById<RecyclerView>(R.id.templates_recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add_template).setOnClickListener {
            showEditDialog(null)
        }

        refreshList()
    }

    private fun refreshList() {
        adapter.submitList(templates.toList())
        val empty = findViewById<View>(R.id.empty_state)
        val recycler = findViewById<View>(R.id.templates_recycler)
        if (templates.isEmpty()) {
            empty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            empty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }
    }

    private fun saveTemplates() {
        settings.sessionTemplates = templates.toList()
    }

    private fun useTemplate(tpl: SessionTemplate) {
        val intent = Intent(this, HostConfigActivity::class.java).apply {
            putExtra(HostConfigActivity.EXTRA_PREFILL_NAME, tpl.name)
            putExtra(HostConfigActivity.EXTRA_PREFILL_HOST, tpl.host)
            putExtra(HostConfigActivity.EXTRA_PREFILL_PORT, tpl.port)
        }
        startActivity(intent)
    }

    private fun showPopupMenu(tpl: SessionTemplate, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Usar modelo")
        popup.menu.add(0, 2, 1, "Editar")
        popup.menu.add(0, 3, 2, "Excluir")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> useTemplate(tpl)
                2 -> showEditDialog(tpl)
                3 -> confirmDelete(tpl)
            }
            true
        }
        popup.show()
    }

    private fun showEditDialog(existing: SessionTemplate?) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val inputName = EditText(this).apply {
            hint = "Nome do modelo"
            existing?.let { setText(it.name) }
        }
        val inputHost = EditText(this).apply {
            hint = "Host (IP ou hostname)"
            existing?.let { setText(it.host) }
        }
        val inputPort = EditText(this).apply {
            hint = "Porta"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(existing?.port?.toString() ?: "23")
        }
        val inputDesc = EditText(this).apply {
            hint = "Descrição (opcional)"
            existing?.let { setText(it.description) }
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(inputName)
            addView(inputHost)
            addView(inputPort)
            addView(inputDesc)
        }

        val title = if (existing == null) "Novo modelo" else "Editar modelo"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val name = inputName.text.toString().trim()
                val host = inputHost.text.toString().trim()
                val port = inputPort.text.toString().toIntOrNull() ?: 23
                val desc = inputDesc.text.toString().trim()
                if (name.isEmpty() || host.isEmpty()) {
                    Toast.makeText(this, "Nome e host são obrigatórios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing == null) {
                    templates.add(SessionTemplate(name = name, host = host, port = port, description = desc))
                } else {
                    val idx = templates.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) templates[idx] = existing.copy(name = name, host = host, port = port, description = desc)
                }
                saveTemplates()
                refreshList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(tpl: SessionTemplate) {
        AlertDialog.Builder(this)
            .setTitle("Excluir modelo")
            .setMessage("Excluir o modelo \"${tpl.name}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                templates.removeAll { it.id == tpl.id }
                saveTemplates()
                refreshList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class TemplatesAdapter(
    private val onUseClick: (SessionTemplate) -> Unit,
    private val onMenuClick: (SessionTemplate, View) -> Unit
) : RecyclerView.Adapter<TemplatesAdapter.VH>() {

    private var list: List<SessionTemplate> = emptyList()

    fun submitList(newList: List<SessionTemplate>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(list[position])
    override fun getItemCount() = list.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.tpl_name)
        private val address: TextView = view.findViewById(R.id.tpl_address)
        private val description: TextView = view.findViewById(R.id.tpl_description)
        private val btnMenu: ImageButton = view.findViewById(R.id.btn_tpl_menu)

        fun bind(tpl: SessionTemplate) {
            name.text = tpl.name
            address.text = "${tpl.host}:${tpl.port}"
            if (tpl.description.isNotBlank()) {
                description.text = tpl.description
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }
            itemView.setOnClickListener { onUseClick(tpl) }
            btnMenu.setOnClickListener { onMenuClick(tpl, btnMenu) }
        }
    }
}
