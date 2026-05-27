package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.ui.HostsViewModel
import com.logisticapp.emuladortelnet.ui.HostsViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class HostsActivity : AppCompatActivity() {

    private lateinit var viewModel: HostsViewModel
    private lateinit var adapter: HostsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val repository = TelnetRepository.getInstance(this)
        viewModel = ViewModelProvider(this, HostsViewModelFactory(repository))
            .get(HostsViewModel::class.java)

        setupRecyclerView()
        observeHosts()

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            openHostConfig(hostId = -1)
        }
    }

    private fun setupRecyclerView() {
        adapter = HostsAdapter(
            onItemClick = { host -> openHostConfig(hostId = host.id) },
            onMenuClick = { host, anchor -> showPopupMenu(host, anchor) }
        )
        val recycler = findViewById<RecyclerView>(R.id.hosts_recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler.adapter = adapter
    }

    private fun observeHosts() {
        lifecycleScope.launch {
            viewModel.hosts.collect { hosts ->
                adapter.submitList(hosts)
                val empty = findViewById<View>(R.id.empty_state)
                val recycler = findViewById<View>(R.id.hosts_recycler)
                if (hosts.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                } else {
                    empty.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                }
                Timber.d("Hosts atualizados: ${hosts.size}")
            }
        }
    }

    private fun showPopupMenu(host: SavedConnection, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Conectar")
        popup.menu.add(0, 2, 1, "Editar")
        popup.menu.add(0, 3, 2, "Configuracao")
        popup.menu.add(0, 4, 3, "Remover")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> connectToHost(host)
                2 -> openHostConfig(hostId = host.id)
                3 -> openHostAdvanced(hostId = host.id)
                4 -> confirmDelete(host)
            }
            true
        }
        popup.show()
    }

    private fun connectToHost(host: SavedConnection) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_HOST, host.host)
            putExtra(MainActivity.EXTRA_PORT, host.port)
            putExtra(MainActivity.EXTRA_NAME, host.name)
            putExtra(MainActivity.EXTRA_HOST_ID, host.id)
        }
        startActivity(intent)
    }

    private fun openHostConfig(hostId: Int) {
        val intent = Intent(this, HostConfigActivity::class.java)
        if (hostId > 0) intent.putExtra(HostConfigActivity.EXTRA_HOST_ID, hostId)
        startActivity(intent)
    }

    private fun openHostAdvanced(hostId: Int) {
        val intent = Intent(this, HostAdvancedActivity::class.java)
        intent.putExtra(HostAdvancedActivity.EXTRA_HOST_ID, hostId)
        startActivity(intent)
    }

    private fun confirmDelete(host: SavedConnection) {
        AlertDialog.Builder(this)
            .setTitle("Remover Host")
            .setMessage("Deseja remover \"${host.name}\"?")
            .setPositiveButton("Remover") { _, _ -> viewModel.deleteHost(host.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

// ------------------------------------------------------------------
// Adapter
// ------------------------------------------------------------------

class HostsAdapter(
    private val onItemClick: (SavedConnection) -> Unit,
    private val onMenuClick: (SavedConnection, View) -> Unit
) : RecyclerView.Adapter<HostsAdapter.VH>() {

    private var list: List<SavedConnection> = emptyList()

    fun submitList(newList: List<SavedConnection>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_host, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.host_name)
        private val address: TextView = view.findViewById(R.id.host_address)
        private val btnMenu: ImageButton = view.findViewById(R.id.btn_menu)

        fun bind(host: SavedConnection) {
            name.text = host.name
            address.text = "${host.host}:${host.port}"
            itemView.setOnClickListener { onItemClick(host) }
            btnMenu.setOnClickListener { onMenuClick(host, btnMenu) }
        }
    }
}
