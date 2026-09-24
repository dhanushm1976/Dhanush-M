package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {

    /**
     * Generates starter PDF study materials if they don't already exist.
     * Returns a list of generated file descriptors with title, file, page count.
     */
    fun ensureSamplePdfs(context: Context): List<SamplePdfInfo> {
        val pdfDir = File(context.filesDir, "study_materials")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val results = mutableListOf<SamplePdfInfo>()

        // 1. Calculus Notes
        val calcFile = File(pdfDir, "Calculus_and_Linear_Algebra_Notes.pdf")
        if (!calcFile.exists()) {
            createStudyPdf(
                file = calcFile,
                title = "Calculus & Linear Algebra Comprehensive Review",
                subjectCode = "MATH 301",
                pages = listOf(
                    PdfPageContent(
                        heading = "1. Fundamental Theorems of Calculus",
                        paragraphs = listOf(
                            "The First Fundamental Theorem states that if f is continuous on [a, b], then the function g defined by g(x) = integral from a to x of f(t)dt is continuous on [a, b] and differentiable on (a, b), with g'(x) = f(x).",
                            "Key Formula: d/dx [∫(a to x) f(t) dt] = f(x)",
                            "The Second Fundamental Theorem states: ∫(a to b) f(x) dx = F(b) - F(a), where F is any antiderivative of f.",
                            "Techniques of Integration:",
                            "• Substitution Rule: ∫ f(g(x))g'(x) dx = ∫ f(u) du where u = g(x)",
                            "• Integration by Parts: ∫ u dv = uv - ∫ v du"
                        )
                    ),
                    PdfPageContent(
                        heading = "2. Matrices, Determinants & Vector Spaces",
                        paragraphs = listOf(
                            "A square matrix A is invertible if and only if det(A) ≠ 0.",
                            "Eigenvalues & Eigenvectors: An eigenvector of an n×n matrix A is a non-zero vector v such that Av = λv for some scalar λ (eigenvalue).",
                            "Characteristic equation: det(A - λI) = 0.",
                            "Orthogonal Projections: proj_u(v) = ((v • u) / (u • u)) * u.",
                            "Singular Value Decomposition (SVD): A = U Σ V^T, decomposing any real m×n matrix into rotation, scaling, and rotation."
                        )
                    ),
                    PdfPageContent(
                        heading = "3. Practice Problems & Key Derivations",
                        paragraphs = listOf(
                            "Problem 1: Evaluate ∫ x * e^(2x) dx using integration by parts.",
                            "Let u = x, dv = e^(2x)dx => du = dx, v = 0.5 * e^(2x).",
                            "Result = 0.5 * x * e^(2x) - 0.25 * e^(2x) + C.",
                            "Problem 2: Find the eigenvalues of matrix A = [[2, 1], [1, 2]].",
                            "det(A - λI) = (2 - λ)^2 - 1 = 0 => (2 - λ) = ±1 => λ1 = 3, λ2 = 1.",
                            "Daily Assignment Checklist: Complete exercises 4.2 to 4.8 from chapter 4."
                        )
                    )
                )
            )
        }
        results.add(SamplePdfInfo("Calculus & Linear Algebra Review", "MATH 301", calcFile, 3))

        // 2. Data Structures & Algorithms
        val dsaFile = File(pdfDir, "Algorithms_and_Data_Structures_CheatSheet.pdf")
        if (!dsaFile.exists()) {
            createStudyPdf(
                file = dsaFile,
                title = "Algorithms & Data Structures Reference",
                subjectCode = "CS 204",
                pages = listOf(
                    PdfPageContent(
                        heading = "1. Asymptotic Complexity (Big-O)",
                        paragraphs = listOf(
                            "Common time complexities from fastest to slowest:",
                            "• O(1) - Constant: Hash table lookup, array index access",
                            "• O(log n) - Logarithmic: Binary search on sorted arrays",
                            "• O(n) - Linear: Linear scan, single traversal",
                            "• O(n log n) - Linearithmic: MergeSort, QuickSort (average), HeapSort",
                            "• O(n²) - Quadratic: BubbleSort, SelectionSort, nested loops",
                            "• O(2ⁿ) - Exponential: Naive Fibonacci, subsets generation"
                        )
                    ),
                    PdfPageContent(
                        heading = "2. Graph Traversal & Shortest Path",
                        paragraphs = listOf(
                            "Breadth-First Search (BFS): Uses a FIFO Queue. Finds shortest path in unweighted graphs. Time: O(V + E).",
                            "Depth-First Search (DFS): Uses a LIFO Stack or recursion. Ideal for cycle detection and topological sorting. Time: O(V + E).",
                            "Dijkstra's Algorithm: Greedily finds shortest path from single source on weighted graphs with non-negative edge weights using Min-Priority Queue. Time: O((V + E) log V).",
                            "Dynamic Programming Pattern: Break problem into overlapping subproblems with optimal substructure. Memoize or Tabulate."
                        )
                    )
                )
            )
        }
        results.add(SamplePdfInfo("Algorithms & Data Structures", "CS 204", dsaFile, 2))

        // 3. Physics II
        val physFile = File(pdfDir, "Electromagnetism_and_Modern_Physics.pdf")
        if (!physFile.exists()) {
            createStudyPdf(
                file = physFile,
                title = "Electromagnetism & Waves Study Guide",
                subjectCode = "PHYS 102",
                pages = listOf(
                    PdfPageContent(
                        heading = "1. Maxwell's Equations in Integral Form",
                        paragraphs = listOf(
                            "1. Gauss's Law: ∮ E • dA = Q_enclosed / ε₀ (Electric flux through closed surface is proportional to enclosed charge).",
                            "2. Gauss's Law for Magnetism: ∮ B • dA = 0 (No magnetic monopoles exist).",
                            "3. Faraday's Law: ∮ E • dl = - dΦ_B / dt (Changing magnetic flux induces EMF).",
                            "4. Ampère-Maxwell Law: ∮ B • dl = μ₀(I_enc + ε₀ dΦ_E / dt).",
                            "Wave Speed: c = 1 / sqrt(μ₀ * ε₀) ≈ 3.0 × 10⁸ m/s."
                        )
                    ),
                    PdfPageContent(
                        heading = "2. Quantum Mechanical Wave-Particle Duality",
                        paragraphs = listOf(
                            "De Broglie Wavelength: λ = h / p, where h = Planck's constant and p = momentum.",
                            "Heisenberg Uncertainty Principle: Δx * Δp ≥ ħ / 2.",
                            "Photoelectric Effect: K_max = h * f - Φ (Work function).",
                            "Energy of a photon: E = h * f = hc / λ.",
                            "Pending Lab Assignment: Submit writeup on Millikan Oil Drop Experiment."
                        )
                    )
                )
            )
        }
        results.add(SamplePdfInfo("Electromagnetism & Modern Physics", "PHYS 102", physFile, 2))

        return results
    }

    /**
     * Generates sample official identity documents (Aadhaar, PAN, Student ID).
     */
    fun ensureSampleIdentityDocs(context: Context): List<SampleIdentityDocInfo> {
        val idDir = File(context.filesDir, "identity_documents")
        if (!idDir.exists()) {
            idDir.mkdirs()
        }

        val results = mutableListOf<SampleIdentityDocInfo>()

        // 1. Aadhaar Card
        val aadhaarFile = File(idDir, "Aadhaar_Card_eAadhaar.pdf")
        if (!aadhaarFile.exists()) {
            createIdentityPdf(
                file = aadhaarFile,
                headerTitle = "GOVERNMENT OF INDIA",
                subHeader = "UNIQUE IDENTIFICATION AUTHORITY OF INDIA (UIDAI)",
                docName = "e-Aadhaar / Digital Identity Card",
                docNumber = "XXXX-XXXX-9842",
                details = listOf(
                    "Name: Student Scholar (Dhanush)",
                    "Date of Birth: 14/08/2003",
                    "Gender: Male",
                    "Aadhaar VID: 9182 3847 2910 4819",
                    "Address: Electronic City Phase 1, Bangalore, Karnataka - 560100",
                    "Mera Aadhaar, Meri Pehchan",
                    "• This electronic document is valid under the Aadhaar Act, 2016.",
                    "• Verified with UIDAI Digital Signature Certificate."
                ),
                watermark = "AADHAAR - GOVT OF INDIA"
            )
        }
        results.add(
            SampleIdentityDocInfo(
                title = "Aadhaar Card (UIDAI)",
                docType = "AADHAAR",
                documentNumber = "XXXX-XXXX-9842",
                issuer = "UIDAI - Govt. of India",
                file = aadhaarFile,
                notes = "Personal Resident National Identity Document with verified biometric record."
            )
        )

        // 2. PAN Card
        val panFile = File(idDir, "PAN_Card_ePAN.pdf")
        if (!panFile.exists()) {
            createIdentityPdf(
                file = panFile,
                headerTitle = "INCOME TAX DEPARTMENT",
                subHeader = "GOVERNMENT OF INDIA",
                docName = "Permanent Account Number Card (e-PAN)",
                docNumber = "ABCDE1234F",
                details = listOf(
                    "Permanent Account Number: ABCDE1234F",
                    "Name: Student Scholar (Dhanush)",
                    "Father's Name: R. Pushpakumar",
                    "Date of Birth: 14/08/2003",
                    "Issuing Authority: Directorate of Income Tax",
                    "Digital Signature: Verified by Protean eGov Technologies Ltd.",
                    "• Valid for all banking, examination, and statutory KYC requirements across India."
                ),
                watermark = "INCOME TAX - PERMANENT ACCOUNT NUMBER"
            )
        }
        results.add(
            SampleIdentityDocInfo(
                title = "Permanent Account Number (PAN)",
                docType = "PAN",
                documentNumber = "ABCDE1234F",
                issuer = "Income Tax Department",
                file = panFile,
                notes = "Official Indian Tax Identification Number & Banking KYC Document."
            )
        )

        // 3. Student College ID Card
        val studentIdFile = File(idDir, "Student_College_ID_Card.pdf")
        if (!studentIdFile.exists()) {
            createIdentityPdf(
                file = studentIdFile,
                headerTitle = "NATIONAL INSTITUTE OF TECHNOLOGY",
                subHeader = "OFFICE OF ACADEMIC AFFAIRS & REGISTRAR",
                docName = "Student Identity & Campus Access Card",
                docNumber = "STU-2026-8904",
                details = listOf(
                    "Student ID / Roll No: STU-2026-8904",
                    "Full Name: Student Scholar (Dhanush)",
                    "Department: Computer Science & Engineering",
                    "Program: B.Tech (4-Year Undergraduate)",
                    "Valid Thru: June 2027",
                    "Blood Group: O+ • Emergency Contact: +91 98765 43210",
                    "Access Privileges: Central Library, CS Labs, Sports Complex",
                    "• This card must be carried at all times on university premises and during semester exams."
                ),
                watermark = "UNIVERSITY STUDENT IDENTITY CARD"
            )
        }
        results.add(
            SampleIdentityDocInfo(
                title = "University Student ID Card",
                docType = "ID_CARD",
                documentNumber = "STU-2026-8904",
                issuer = "National Institute of Technology",
                file = studentIdFile,
                notes = "Official college campus identification, laboratory access, and semester exam pass."
            )
        )

        return results
    }

    fun createIdentityPdf(
        file: File,
        headerTitle: String,
        subHeader: String,
        docName: String,
        docNumber: String,
        details: List<String>,
        watermark: String
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val pdfPage = document.startPage(pageInfo)
        val canvas = pdfPage.canvas

        canvas.drawColor(Color.WHITE)

        val paintBorder = Paint().apply {
            color = Color.rgb(37, 99, 235)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val paintHeaderBar = Paint().apply {
            color = Color.rgb(238, 242, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintSubHeader = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            isAntiAlias = true
        }

        val paintDocName = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintDocNumber = Paint().apply {
            color = Color.rgb(180, 83, 9)
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11.5f
            isAntiAlias = true
        }

        val paintDivider = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val paintWatermark = Paint().apply {
            color = Color.argb(20, 37, 99, 235)
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawRect(24f, 24f, (pageWidth - 24).toFloat(), (pageHeight - 24).toFloat(), paintBorder)
        canvas.drawRect(26f, 26f, (pageWidth - 26).toFloat(), 110f, paintHeaderBar)

        canvas.drawText(headerTitle, 40f, 60f, paintHeader)
        canvas.drawText(subHeader, 40f, 85f, paintSubHeader)

        var yPos = 145f
        canvas.drawText(docName, 40f, yPos, paintDocName)
        yPos += 28f
        canvas.drawText("DOCUMENT NO: $docNumber", 40f, yPos, paintDocNumber)
        yPos += 20f

        canvas.drawLine(40f, yPos, (pageWidth - 40).toFloat(), yPos, paintDivider)
        yPos += 30f

        canvas.drawText(watermark, 40f, 400f, paintWatermark)

        for (line in details) {
            canvas.drawText(line, 45f, yPos, paintBody)
            yPos += 24f
        }

        val emblemTop = yPos + 20f
        val emblemPaint = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val emblemBorder = Paint().apply {
            color = Color.rgb(34, 197, 94)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val emblemText = Paint().apply {
            color = Color.rgb(21, 128, 61)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawRoundRect(40f, emblemTop, (pageWidth - 40).toFloat(), emblemTop + 50f, 8f, 8f, emblemPaint)
        canvas.drawRoundRect(40f, emblemTop, (pageWidth - 40).toFloat(), emblemTop + 50f, 8f, 8f, emblemBorder)
        canvas.drawText("✓ DIGITALLY VERIFIED AND STORED IN SECURE VAULT", 60f, emblemTop + 30f, emblemText)

        val footerText = "StudySync Official Identity Vault  •  Confidential & Partitioned"
        canvas.drawLine(40f, (pageHeight - 50).toFloat(), (pageWidth - 40).toFloat(), (pageHeight - 50).toFloat(), paintDivider)
        canvas.drawText(footerText, 40f, (pageHeight - 30).toFloat(), paintSubHeader)

        document.finishPage(pdfPage)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun createStudyPdf(file: File, title: String, subjectCode: String, pages: List<PdfPageContent>) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width in postscript points (72 dpi)
        val pageHeight = 842 // Standard A4 height

        val paintTitle = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintSubject = Paint().apply {
            color = Color.rgb(13, 148, 136) // Teal
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintHeading = Paint().apply {
            color = Color.rgb(15, 23, 42) // Dark Slate
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate
            textSize = 11.5f
            isAntiAlias = true
        }

        val paintDivider = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1.5f
        }

        val paintFooter = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 10f
            isAntiAlias = true
        }

        val paintHeaderBar = Paint().apply {
            color = Color.rgb(238, 242, 255)
        }

        for (pageIdx in pages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // Header banner background
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paintHeaderBar)
            canvas.drawLine(0f, 90f, pageWidth.toFloat(), 90f, paintDivider)

            // Header text
            canvas.drawText(subjectCode, 36f, 32f, paintSubject)
            canvas.drawText(title, 36f, 62f, paintTitle)

            val pageContent = pages[pageIdx]
            var yPos = 130f

            // Section heading
            canvas.drawText(pageContent.heading, 36f, yPos, paintHeading)
            yPos += 20f
            canvas.drawLine(36f, yPos, (pageWidth - 36).toFloat(), yPos, paintDivider)
            yPos += 25f

            // Paragraphs
            for (para in pageContent.paragraphs) {
                // simple line wrapping for ~70 chars
                val words = para.split(" ")
                var currentLine = StringBuilder()
                for (w in words) {
                    if (currentLine.length + w.length > 68) {
                        canvas.drawText(currentLine.toString(), 40f, yPos, paintBody)
                        yPos += 18f
                        currentLine = StringBuilder()
                    }
                    if (currentLine.isNotEmpty()) currentLine.append(" ")
                    currentLine.append(w)
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), 40f, yPos, paintBody)
                    yPos += 24f
                }
            }

            // Footer
            val footerText = "StudySync Material Vault  •  Page ${pageIdx + 1} of ${pages.size}"
            canvas.drawLine(36f, (pageHeight - 45).toFloat(), (pageWidth - 36).toFloat(), (pageHeight - 45).toFloat(), paintDivider)
            canvas.drawText(footerText, 36f, (pageHeight - 25).toFloat(), paintFooter)

            document.finishPage(pdfPage)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }
}

data class PdfPageContent(
    val heading: String,
    val paragraphs: List<String>
)

data class SamplePdfInfo(
    val title: String,
    val subjectCode: String,
    val file: File,
    val pageCount: Int
)

data class SampleIdentityDocInfo(
    val title: String,
    val docType: String,
    val documentNumber: String,
    val issuer: String,
    val file: File,
    val notes: String
)
