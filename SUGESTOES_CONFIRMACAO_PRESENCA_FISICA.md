# 💡 SUGESTÕES PARA CONFIRMAÇÃO DE PRESENÇA FÍSICA NO ATO DA ASSINATURA

## Objetivo
Implementar mecanismo robusto para confirmar que o locatário está presente fisicamente durante a assinatura do contrato, conforme Cláusula 9.3(e) e requisitos da Lei 14.063/2020.

---

## 🎯 OPÇÕES DE IMPLEMENTAÇÃO (Do Mais Simples ao Mais Robusto)

### **OPÇÃO 1: Checkbox com Declaração (SIMPLES - Recomendado para início)**

#### Descrição
Checkbox obrigatório antes de salvar a assinatura, onde o representante da empresa confirma a presença física.

#### Implementação

**1. Layout (`fragment_signature_capture.xml`):**
```xml
<!-- Adicionar ANTES do botão "Salvar Assinatura" -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="16dp"
    android:layout_marginBottom="16dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Confirmação de Presença Física"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="@color/primary_color"
            android:layout_marginBottom="12dp" />

        <com.google.android.material.checkbox.MaterialCheckBox
            android:id="@+id/checkboxPresencaFisica"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Confirmo que o(a) locatário(a) está presente fisicamente e assinando pessoalmente este contrato"
            android:textSize="14sp"
            android:checked="false" />

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilConfirmadoPor"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Nome de quem confirma"
            app:startIconDrawable="@drawable/ic_person"
            app:helperText="Nome completo do representante que presenciou">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etConfirmadoPor"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPersonName"
                android:maxLines="1" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilConfirmadoPorCpf"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="CPF de quem confirma"
            app:startIconDrawable="@drawable/ic_id_card"
            app:helperText="CPF do representante">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etConfirmadoPorCpf"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="number"
                android:maxLines="1" />
        </com.google.android.material.textfield.TextInputLayout>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

**2. Código (`SignatureCaptureFragment.kt`):**
```kotlin
private fun salvarAssinatura() {
    // ... código existente de validação da assinatura ...
    
    // ✅ NOVO: Validar confirmação de presença física
    if (!binding.checkboxPresencaFisica.isChecked) {
        Toast.makeText(requireContext(), 
            "É obrigatório confirmar a presença física do locatário", 
            Toast.LENGTH_LONG).show()
        return
    }
    
    val confirmadoPor = binding.etConfirmadoPor.text.toString().trim()
    val confirmadoPorCpf = binding.etConfirmadoPorCpf.text.toString().trim()
    
    if (confirmadoPor.isEmpty()) {
        binding.tilConfirmadoPor.error = "Nome é obrigatório"
        return
    }
    
    if (confirmadoPorCpf.isEmpty() || !validarCPF(confirmadoPorCpf)) {
        binding.tilConfirmadoPorCpf.error = "CPF válido é obrigatório"
        return
    }
    
    // ... código existente de captura de metadados ...
    
    // ✅ NOVO: Salvar confirmação de presença física
    viewModel.salvarAssinaturaComMetadados(
        assinaturaBase64 = assinaturaBase64,
        hashAssinatura = signatureHash,
        deviceId = metadata.deviceId,
        ipAddress = metadata.ipAddress,
        timestamp = metadata.timestamp,
        pressaoMedia = statistics.averagePressure,
        velocidadeMedia = statistics.averageVelocity,
        duracao = statistics.duration,
        totalPontos = statistics.totalPoints,
        presencaFisicaConfirmada = true,
        presencaFisicaConfirmadaPor = confirmadoPor,
        presencaFisicaConfirmadaCpf = confirmadoPorCpf
    )
}

private fun validarCPF(cpf: String): Boolean {
    val cpfLimpo = cpf.replace(Regex("[^0-9]"), "")
    return cpfLimpo.length == 11
}
```

**3. Atualizar ViewModel:**
```kotlin
fun salvarAssinaturaComMetadados(
    // ... parâmetros existentes ...
    presencaFisicaConfirmada: Boolean = false,
    presencaFisicaConfirmadaPor: String? = null,
    presencaFisicaConfirmadaCpf: String? = null
) {
    viewModelScope.launch {
        // ... código existente ...
        val contratoAtualizado = contrato.copy(
            // ... campos existentes ...
            presencaFisicaConfirmada = presencaFisicaConfirmada,
            presencaFisicaConfirmadaPor = presencaFisicaConfirmadaPor,
            presencaFisicaConfirmadaCpf = presencaFisicaConfirmadaCpf,
            presencaFisicaConfirmadaTimestamp = System.currentTimeMillis()
        )
        // ... resto do código ...
    }
}
```

**Vantagens:**
- ✅ Simples de implementar
- ✅ Não requer permissões adicionais
- ✅ Rápido para o usuário
- ✅ Cria evidência documental

**Desvantagens:**
- ⚠️ Depende da honestidade do representante
- ⚠️ Não tem evidência visual (foto)

---

### **OPÇÃO 2: Diálogo de Confirmação com Foto (MÉDIO - Recomendado)**

#### Descrição
Diálogo modal que aparece antes de salvar, solicitando confirmação e permitindo tirar foto do locatário assinando.

#### Implementação

**1. Criar layout do diálogo (`dialog_confirmar_presenca_fisica.xml`):**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Confirmação de Presença Física"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="@color/primary_color"
        android:gravity="center"
        android:layout_marginBottom="16dp" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Para garantir a validade jurídica do contrato, confirme que o(a) locatário(a) está presente fisicamente."
        android:textSize="14sp"
        android:textColor="@color/text_secondary"
        android:layout_marginBottom="16dp" />

    <!-- Foto do locatário assinando (opcional mas recomendado) -->
    <com.google.android.material.card.MaterialCardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        app:cardCornerRadius="8dp"
        app:cardElevation="2dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="12dp">

            <ImageView
                android:id="@+id/ivFotoPresenca"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:scaleType="centerCrop"
                android:background="@color/background_light"
                android:contentDescription="Foto do locatário assinando"
                android:visibility="gone" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnTirarFoto"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="📷 Tirar Foto do Locatário Assinando"
                android:textSize="14sp"
                app:icon="@drawable/ic_camera"
                style="@style/Widget.Material3.Button.OutlinedButton" />

        </LinearLayout>

    </com.google.android.material.card.MaterialCardView>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilConfirmadoPor"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Nome de quem confirma"
        app:startIconDrawable="@drawable/ic_person"
        app:helperText="Nome completo do representante">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etConfirmadoPor"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPersonName" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/tilConfirmadoPorCpf"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="CPF de quem confirma"
        app:startIconDrawable="@drawable/ic_id_card">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etConfirmadoPorCpf"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="number" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.checkbox.MaterialCheckBox
        android:id="@+id/checkboxConfirmar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Confirmo que o(a) locatário(a) está presente e assinando pessoalmente"
        android:textSize="14sp" />

</LinearLayout>
```

**2. Criar DialogFragment (`ConfirmarPresencaFisicaDialog.kt`):**
```kotlin
class ConfirmarPresencaFisicaDialog : DialogFragment() {
    
    interface OnPresencaConfirmadaListener {
        fun onPresencaConfirmada(
            confirmadoPor: String,
            confirmadoPorCpf: String,
            fotoUri: Uri?
        )
    }
    
    private var listener: OnPresencaConfirmadaListener? = null
    private var fotoUri: Uri? = null
    private val REQUEST_IMAGE_CAPTURE = 100
    
    fun setOnPresencaConfirmadaListener(listener: OnPresencaConfirmadaListener) {
        this.listener = listener
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogConfirmarPresencaFisicaBinding.inflate(layoutInflater)
        
        binding.btnTirarFoto.setOnClickListener {
            tirarFoto()
        }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Confirmação de Presença Física")
            .setPositiveButton("Confirmar") { _, _ ->
                val confirmadoPor = binding.etConfirmadoPor.text.toString().trim()
                val confirmadoPorCpf = binding.etConfirmadoPorCpf.text.toString().trim()
                
                if (!binding.checkboxConfirmar.isChecked) {
                    Toast.makeText(requireContext(), 
                        "É obrigatório confirmar a presença física", 
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (confirmadoPor.isEmpty() || confirmadoPorCpf.isEmpty()) {
                    Toast.makeText(requireContext(), 
                        "Preencha todos os campos", 
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                listener?.onPresencaConfirmada(confirmadoPor, confirmadoPorCpf, fotoUri)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
    
    private fun tirarFoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                fotoUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }
    
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PRESENCA_${timeStamp}_", ".jpg", storageDir)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            fotoUri?.let { uri ->
                binding.ivFotoPresenca.setImageURI(uri)
                binding.ivFotoPresenca.visibility = View.VISIBLE
            }
        }
    }
}
```

**Vantagens:**
- ✅ Evidência visual (foto)
- ✅ Mais robusto juridicamente
- ✅ Foto pode ser anexada ao PDF do contrato

**Desvantagens:**
- ⚠️ Requer permissão de câmera
- ⚠️ Mais complexo de implementar
- ⚠️ Pode ser invasivo para o locatário

---

### **OPÇÃO 3: Confirmação com Geolocalização (AVANÇADO)**

#### Descrição
Além da confirmação, captura a localização GPS no momento da assinatura para comprovar que ambas as partes estavam no mesmo local.

#### Implementação

**1. Adicionar campos na entidade:**
```kotlin
val presencaFisicaLatitude: Double? = null,
val presencaFisicaLongitude: Double? = null,
val presencaFisicaEndereco: String? = null, // Endereço obtido via reverse geocoding
```

**2. Solicitar permissão de localização:**
```kotlin
private fun solicitarLocalizacao() {
    if (ContextCompat.checkSelfPermission(requireContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 
            REQUEST_LOCATION_PERMISSION)
    } else {
        obterLocalizacao()
    }
}

private fun obterLocalizacao() {
    val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        // Usar FusedLocationProviderClient para obter localização
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                // Fazer reverse geocoding para obter endereço
                obterEndereco(latitude, longitude)
            }
        }
    }
}
```

**Vantagens:**
- ✅ Evidência geográfica forte
- ✅ Comprova que ambas partes estavam no mesmo local
- ✅ Útil para contratos assinados em locais específicos

**Desvantagens:**
- ⚠️ Requer permissão de localização
- ⚠️ Pode não funcionar bem em ambientes fechados
- ⚠️ Questões de privacidade

---

### **OPÇÃO 4: Confirmação com Código SMS/WhatsApp (MUITO ROBUSTO)**

#### Descrição
Envia código de verificação via SMS ou WhatsApp para o telefone do locatário. O locatário deve informar o código para confirmar presença.

#### Implementação

**1. Fluxo:**
1. Representante solicita código de verificação
2. Sistema envia código via WhatsApp/SMS para o telefone do locatário
3. Locatário informa o código recebido
4. Sistema valida código e confirma presença física

**2. Adicionar campos:**
```kotlin
val presencaFisicaCodigoVerificacao: String? = null,
val presencaFisicaCodigoEnviadoEm: Long? = null,
val presencaFisicaCodigoValidadoEm: Long? = null,
```

**Vantagens:**
- ✅ Muito robusto juridicamente
- ✅ Comprova que o locatário tem acesso ao telefone cadastrado
- ✅ Dificulta fraude

**Desvantagens:**
- ⚠️ Requer integração com API de SMS/WhatsApp
- ⚠️ Pode ter custos
- ⚠️ Mais complexo de implementar
- ⚠️ Depende de sinal de telefone

---

## 🎯 RECOMENDAÇÃO FINAL

### **Implementação em Fases:**

#### **FASE 1 (Imediata - Opção 1):**
- ✅ Checkbox obrigatório com declaração
- ✅ Campos de nome e CPF de quem confirma
- ✅ Validação antes de salvar assinatura
- ✅ Timestamp automático

**Tempo de implementação:** 1-2 horas

#### **FASE 2 (Curto Prazo - Opção 2):**
- ✅ Adicionar opção de tirar foto (opcional)
- ✅ Armazenar foto junto com o contrato
- ✅ Incluir foto no PDF do contrato

**Tempo de implementação:** 3-4 horas

#### **FASE 3 (Médio Prazo - Opção 3):**
- ✅ Adicionar geolocalização (opcional)
- ✅ Reverse geocoding para endereço
- ✅ Exibir localização no PDF

**Tempo de implementação:** 4-6 horas

#### **FASE 4 (Longo Prazo - Opção 4):**
- ✅ Implementar verificação por código SMS/WhatsApp
- ✅ Integração com API de envio
- ✅ Validação de código

**Tempo de implementação:** 1-2 dias

---

## 📋 CHECKLIST DE IMPLEMENTAÇÃO (Fase 1 - Recomendada)

### 1. Atualizar Layout
- [ ] Adicionar card de confirmação no `fragment_signature_capture.xml`
- [ ] Adicionar checkbox obrigatório
- [ ] Adicionar campos de nome e CPF
- [ ] Adicionar validação visual

### 2. Atualizar Fragment
- [ ] Adicionar validação antes de salvar
- [ ] Validar CPF (formato básico)
- [ ] Passar dados para ViewModel

### 3. Atualizar ViewModel
- [ ] Adicionar parâmetros de confirmação
- [ ] Salvar dados no contrato

### 4. Atualizar PDF
- [ ] Adicionar seção de confirmação de presença física no PDF
- [ ] Exibir nome, CPF e data/hora da confirmação

### 5. Testes
- [ ] Testar fluxo completo
- [ ] Validar que não salva sem confirmação
- [ ] Verificar dados no banco
- [ ] Verificar PDF gerado

---

## 🔒 BOAS PRÁTICAS JURÍDICAS

### 1. **Declaração Clara e Explícita**
- Texto deve ser claro: "Confirmo que o(a) locatário(a) está presente fisicamente..."
- Não usar linguagem ambígua

### 2. **Identificação do Confirmante**
- Sempre solicitar nome completo
- Sempre solicitar CPF (para identificação única)
- Validar formato do CPF

### 3. **Timestamp Preciso**
- Registrar timestamp no momento exato da confirmação
- Usar timezone correto
- Não permitir edição posterior

### 4. **Evidências Adicionais (Recomendado)**
- Foto do locatário assinando
- Geolocalização do local
- Código de verificação via SMS/WhatsApp

### 5. **Documentação no PDF**
- Incluir seção dedicada no PDF do contrato
- Exibir todos os dados de confirmação
- Formato profissional e legível

---

## 📄 EXEMPLO DE TEXTO PARA O PDF

```
CONFIRMAÇÃO DE PRESENÇA FÍSICA

Eu, [NOME DO REPRESENTANTE], CPF [CPF], funcionário da LOCADORA, 
confirmo que o(a) LOCATÁRIO(A) [NOME DO LOCATÁRIO], CPF [CPF], 
esteve presente fisicamente no momento da assinatura deste contrato, 
realizada em [DATA E HORA], no endereço [ENDEREÇO - se houver geolocalização].

Assinatura do Representante: _________________

Data/Hora da Confirmação: [TIMESTAMP FORMATADO]
```

---

## ⚖️ VALIDADE JURÍDICA

### Conformidade com Lei 14.063/2020:
- ✅ **Identificação do signatário**: CPF do locatário confirmado
- ✅ **Rastreabilidade**: Timestamp e dados do confirmante
- ✅ **Evidência de presença**: Declaração explícita + foto (opcional)

### Fortalecimento da Prova:
- ✅ **Declaração escrita**: Checkbox com texto claro
- ✅ **Identificação do testemunha**: Nome e CPF do representante
- ✅ **Timestamp preciso**: Momento exato da confirmação
- ✅ **Evidência visual**: Foto (se implementada)
- ✅ **Evidência geográfica**: Localização GPS (se implementada)

---

## 🚀 PRÓXIMOS PASSOS

1. **Decidir qual opção implementar** (recomendo começar com Opção 1)
2. **Implementar Fase 1** (checkbox + campos)
3. **Testar em ambiente de desenvolvimento**
4. **Validar com advogado** (se possível)
5. **Implementar melhorias adicionais** (Fases 2, 3, 4)

---

**Documento criado em:** 2025  
**Versão:** 1.0  
**Status:** Propostas de implementação

