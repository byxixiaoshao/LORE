package com.bicy.novel.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.data.export.*
import com.bicy.novel.ui.components.ExportOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportState(
    val isExporting: Boolean = false,
    val showExportDialog: Boolean = false,
    val novelId: Long = 0,
    val result: ExportResult? = null
)

data class ImportState(
    val isImporting: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val importUri: Uri? = null,
    val result: ImportResult? = null
)

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    private val exportService: ProjectExportService,
    private val txtExportService: TxtExportService,
    private val importService: ProjectImportService
) : ViewModel() {
    
    private val _exportState = MutableStateFlow(ExportState())
    val exportState = _exportState.asStateFlow()
    
    private val _importState = MutableStateFlow(ImportState())
    val importState = _importState.asStateFlow()
    
    fun showExportDialog(novelId: Long) {
        _exportState.value = ExportState(
            showExportDialog = true,
            novelId = novelId
        )
    }
    
    fun hideExportDialog() {
        _exportState.value = ExportState()
    }
    
    fun exportProject(
        uri: Uri,
        options: ExportOptions
    ) {
        viewModelScope.launch {
            _exportState.value = _exportState.value.copy(
                isExporting = true
            )
            
            val result = if (options.isBackup) {
                // JSON备份格式
                exportService.exportProject(
                    novelId = _exportState.value.novelId,
                    outputUri = uri,
                    password = options.password.ifBlank { null },
                    includeChapters = options.includeChapters,
                    includeCharacters = options.includeCharacters,
                    includeWorldviews = options.includeWorldviews,
                    includeNotes = options.includeNotes,
                    includeTimeline = options.includeTimeline
                )
            } else {
                // TXT导出格式
                txtExportService.exportAsTxt(
                    novelId = _exportState.value.novelId,
                    outputUri = uri,
                    includeChapters = options.includeChapters,
                    includeCharacters = options.includeCharacters,
                    includeWorldviews = options.includeWorldviews,
                    includeNotes = options.includeNotes,
                    includeTimeline = options.includeTimeline
                )
            }
            
            _exportState.value = _exportState.value.copy(
                isExporting = false,
                showExportDialog = false,
                result = result
            )
        }
    }
    
    fun clearExportResult() {
        _exportState.value = _exportState.value.copy(result = null)
    }
    
    fun startImport(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState(
                isImporting = true,
                importUri = uri
            )
            
            val meta = importService.checkImportFile(uri)
            
            if (meta.encrypted) {
                _importState.value = _importState.value.copy(
                    isImporting = false,
                    showPasswordDialog = true
                )
            } else {
                val result = importService.importProject(uri)
                _importState.value = _importState.value.copy(
                    isImporting = false,
                    result = result
                )
            }
        }
    }
    
    fun importWithPassword(password: String) {
        viewModelScope.launch {
            _importState.value = _importState.value.copy(
                isImporting = true,
                showPasswordDialog = false
            )
            
            val result = importService.importProject(
                inputUri = _importState.value.importUri!!,
                password = password
            )
            
            _importState.value = _importState.value.copy(
                isImporting = false,
                result = result
            )
        }
    }
    
    fun hidePasswordDialog() {
        _importState.value = _importState.value.copy(
            showPasswordDialog = false
        )
    }
    
    fun clearImportResult() {
        _importState.value = ImportState()
    }
}
