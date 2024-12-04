$(document).ready(function () {

    $('.btn-theme-change').on('click', function (e) {
        var htmlElement = document.documentElement;
        var currentTheme = htmlElement.getAttribute('data-fr-theme');
        var newTheme = (currentTheme == 'light') ? 'dark' : 'light';
        htmlElement.setAttribute('data-fr-theme', newTheme);
        htmlElement.setAttribute('data-bs-theme', newTheme);
        localStorage.setItem('fr-theme', newTheme);
    });

    $('.btn-modal-confirm-before-submit').on('click', function (e) {
        e.preventDefault();
        var form = $(this).closest('form');
        var modal = form.find('.modal');
        $(modal).modal('show');
    });

    $('.btnPartner').on('click', function (e) {
        e.preventDefault();

        var href = $(this).attr('href');

        $('#hrefPartner').attr('href', href);
        $('#triggerPart').modal('show');

    })

    $('.deleteButton').on('click', function (e) {
        e.preventDefault();

        var href = $(this).attr('href');
        var id = $(this).attr('data-id');

        $('#deleteRef' + id).attr('href', href);
        $('#deleteModal' + id).modal('show');

    })

    $('.deleteTenant').on('click', function (e) {
        e.preventDefault();

        var href = $(this).attr('href');

        $('#idRef').attr('href', href);
        $('#deleteTenant').modal('show');

    })

    $('.btn-deleteCotenant').on('click', function (e) {
        e.preventDefault();

        var href = $(this).attr('href');
        var id = $(this).attr('data-id');

        $('#deleteRefCo' + id).attr('href', href);
        $('#deleteCotenant' + id).modal('show');

    })

    $('.btn-deleteGuarantor').on('click', function (e) {
        e.preventDefault();
        const guarantorId = $(this).attr('guarantor-id');
        $('#modalDeleteGuarantor-' + guarantorId).modal('show');
    })

    $('.deleteApartmentSharing').on('click', function (e) {
        e.preventDefault();
        var href = $(this).attr('href');
        $('#idRefApt').attr('href', href);
        $('#deleteApartmentSharing').modal('show');

    })


    $('#editProperty').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget); // Button that triggered the modal
        var name = button.data('name');
        var agentEmail = button.data('agent-email');
        var propertyId = button.data('property-id');
        var rentCost = button.data('rent-cost');
        var id = button.data('id');
        // If necessary, you could initiate an AJAX request here (and then do the updating in a callback).
        // Update the modal's content. We'll use jQuery here, but you could use a data binding library or other methods instead.
        var modal = $(this);
        modal.find('#name').val(name);
        modal.find('#propertyId').val(propertyId);
        modal.find('#rentCost').val(rentCost);
        modal.find('#agentId option').filter(function () {
            return $(this).text() === agentEmail;
        }).prop("selected", true);
        modal.find("#buttonMerge").attr("data-id", id);
        modal.find("#editPropertyForm").attr('action', '/bo/properties-pro/' + id + '/update');
    });

    $('#deleteProperty').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var id = button.data('id');
        var modal = $(this);
        modal.find('#deletePropertyForm').attr('action', '/bo/properties-pro/' + id + '/delete')
    });

    $("#mergeProperty").on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var id = button.data('id');
        $.ajax({
            url: "/bo/properties-agent/" + id,
            type: "GET",
            processData: false,
            contentType: false,
            cache: false,
            success: function (data) {
                var array = JSON.parse(data);
                $.each(array, function () {
                    $("#propertyIdDestiny").append(new Option(this.propertyId, this.id));
                });
                $("#mergePropertyForm").attr("action", "/bo/properties-pro/merge/" + id);
            }
        });
    });

    $("#date-statistic").change(function (e) {
        modifyHref();
    });

    $("#days-statistic").change(function (e) {
        modifyHref();
    });

    $("#admins").change(function (e) {
        modifyHref();
    });

    function modifyHref() {
        var date = $("#date-statistic").val();
        var days = $("#days-statistic").val();
        var href = "/bo/statistic/admin?date=" + date + "&days=" + days;
        $("#admins option:selected").each(function (e) {
            href += "&user=" + $(this).val();
        });
        $("#link-statistic").attr("href", href)

    }

    // Javascript to enable link to tab
    var url = document.location.toString();
    if (url.match('#')) {
        $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
    }


    $("#file5GeneratedM").change(function (e) {
        $("#inputFile5").toggle();
    });

    if ($("#file5GeneratedM").is(":checked")) {
        $("#inputFile5").toggle();
    }

    $(".file-process-result-link").click(function (e) {
        e.preventDefault();
        var id = $(this).attr('data-id');
        $("#file-process-result-div" + id).load("/bo/tenant/" + id + "/showResult").fadeOut('fast', function () {
            $("#file-process-result-div" + id).fadeIn('fast');
            $(".ing").click(function (e) {
                e.preventDefault();
            })
        })
    });
    /*
        $('.data-tab2').click(function (e) {
            var id = $(this).attr('data-name');
            updateMessagesAdmin2(id)
        });
    */
    $('.btn-denied-form').click(function (e) {
        var id = $(this).attr('data-id');
        var idApt = $(this).attr('data-idApt');
        $('#form-denied' + id).submit(function (e) {
            sendNewMessage2(e, "/bo/tenant/" + id + "/decline", $(this), function () {
                window.location.reload(true);
            })
        })
    })

    $('.btn-validate-form').click(function (e) {
        var id = $(this).attr('data-id');
        var idApt = $(this).attr('data-idApt');

        $('#form-validated' + id).submit(function (e) {
            sendNewMessage3(e, "/bo/tenant/" + id + "/validate", $(this), function () {
                window.location.reload(true);
            })
        })
    })

    $('.select-change-document-status').on('change', function () {
        const documentId = $(this).attr('document-id');
        $('#form-change-document-status' + documentId).submit();
    })

    function updateMessageForm(target) {
        var id = target.attr('data-tenant-id');
        var nameAdmin = target.attr('data-nameAdmin');
        $('#tenant-message' + id).load("/bo/message/tenant/" + id, function () {
            $("#messageForm" + id).submit(function (e) {
                    var id1 = $(this).attr('data-tenant-id');
                    sendNewMessage(e, "/bo/message/new/" + id, $(this), nameAdmin, id, function () {
                        location.reload();
                    })
                }
            );
        });
    }
    $('.chat').click(function (e) {
        updateMessageForm($(this));
    });

    $('.chat').each(function(index) {
        if ($(this) !== undefined && $(this).attr('aria-selected') === 'true') {
            updateMessageForm($(this));
        }
    })

    $(document).keydown(function (event) {
        var flag = true;
        if (event.ctrlKey && event.shiftKey && event.keyCode === 13) {
            event.preventDefault();
            flag = false;
            $('#formChat').attr('data-next', true);
            $('#formChat').click();
        }
        if (event.ctrlKey && event.keyCode == 13 && flag) {
            $('#formChat').click();
            event.preventDefault();
        }
    });

    var currentTab = $("#current-tab");
    if (currentTab.val() != '') {
        if ($('.nav-tabs a[href="#' + currentTab.val() + '"]').length > 0) {
            $('.nav-tabs a[href="#' + currentTab.val() + '"]').tab('show');
            $('.nav-tabs a[href="#' + currentTab.val() + '"]').click();
        }
    }

    $(".ing").click(function (e) {
        e.preventDefault();
    });
    changePageAndSize();
    $("#formLocataire").fadeIn("slow");
    if ($("#formLocataire").attr('data-type') === 'CREATE') {
        $("#formLocataire").attr('action', '/creer-compte/create-colocation')
    } else if ($("#formLocataire").attr('data-type') == 'JOIN') {
        $("#formLocataire").attr('action', '/creer-compte/join-colocation')
    }


    function formControlTouched() {
        $(".form-control").keyup(function (e) {
            if (e.target.value) {
                $(this).removeClass("form-control-untouched").removeClass("invalid")
            } else {
                $(this).addClass("form-control-untouched")
            }
        })
    }

    formControlTouched();

    if ($("#file4Generated").is(":checked")) {
        $("#inputFile4").toggle();
        $("#causeFile4Generated").toggle();
    }
    $("#file4Generated").change(function (e) {
        $("#checkboxAcceptVerification").toggle();
        $("#inputFile4").toggle();
        $("#causeFile4Generated").toggle();
        $("#file4").addClass('no-validate');
        $("#acceptVerification").prop('checked', this.checked);
    });

    if ($("#file5Generated").is(":checked")) {
        $(".inputFile5").toggle();
    }

    $("#file5Generated").change(function (e) {
        $(".inputFile5").toggle();
        $(".file5").addClass('no-validate');
    });

    var initValueFile4Generated = $("#file4Generated").is(":checked");
    var initSalary = $("#salary").val();
    var initValueFile5Generated = $("#file5GeneratedM").is(":checked");

    $(".modify").click(function (e) {

        var valueFile4Generated = $("#file4Generated").is(":checked");
        var valueFile5Generated = $("#file5GeneratedM").is(":checked");
        var salary = $("#salary").val();
        //var valueFile4Cause =  $("#causeFile4Generated input:checked[type='radio']").val();
        var file4 = false;
        var file5 = false;
        if (!initValueFile4Generated && valueFile4Generated) {
            file4 = true;
            $("#textGeneratedFile4").show();
        } else {
            file4 = false;
            $("#textGeneratedFile4").hide();
        }
        if (initSalary != 0 && salary == 0) {
            file5 = true;
            $("#textGeneratedFile5").show();
        } else {
            file5 = false;
            $("#textGeneratedFile5").hide();
        }
        if (!initValueFile5Generated && valueFile5Generated) {
            file5 = true;
            $("#textGeneratedFile5").show();
        } else {
            file5 = false;
            $("#textGeneratedFile5").hide();
        }
        if (file4 || file5) {
            e.preventDefault();
            $('#confirmFile4Generated').modal('show');
            $("#confirmFile4GeneratedYES").click(function (e) {
                e.preventDefault();
                $("#formLocataire").submit();
            });
        }
    });

    if ($(".dropdown-menu").length) {
        $(".dropdown-menu").on("show.bs.dropdown", function (event) {
            var src = $(event.relatedTarget)[0];
            src.dataset.toggle = "tab";
            src.dataset.target = "#properties,#no-title";
            src.tab("show");
            src.dataset.toggle = "dropdown";
            src.dataset.target = null;
        });
    }

    $(".closebtnaction").click(function (e) {
        e.preventDefault();
        $('#link-info').modal('hide');
        $('#link-info2').modal('hide');
    });

    $("#email").focus(function () {
        $("#email-error").hide();
    });
    $("#lastName").focus(function () {
        $("#lastName-error").hide();
    });
    $("#firstName").focus(function () {
        $("#firstName-error").hide();
    });

    $("#messageForm").submit(function (e) {

        sendNewMessage(e, "/message/new", $(this), name, '');
    });


    function sendNewMessage(e, url, form, name, id, next) {
        e.preventDefault();
        $.ajax({
            url: url,
            type: "POST",
            data: new FormData(form[0]),
            processData: false,
            contentType: false,
            cache: false,
            success:
                function (data) {
                    next && next();

                },
            error: function (data) {
                //$("#tenant-message" + id).load("/bo/message/tenant/" + id);

            }
        });

    }

    function sendNewMessage2(e, url, form, next) {
        e.preventDefault();
        $.ajax({
            url: url,
            type: "POST",
            data: new FormData(form[0]),
            processData: false,
            contentType: false,
            cache: false,
            success:
                function () {
                    next && next();
                },
            error: function (data) {
                console.log('error :' + data)
            }
        });

    }

    function sendNewMessage3(e, url, form, next) {
        e.preventDefault();
        $.ajax({
            url: url,
            type: "POST",
            data: new FormData(form[0]),
            processData: false,
            contentType: false,
            cache: false,
            success:
                function () {
                    next && next();
                },
            error: function (data) {
                console.log('error :' + data)


            }
        });

    }

    function enableTooltips() {
        $('[data-toggle="tooltip"]').tooltip();
    }

    enableTooltips();

    function addFileReaderInput() {
        $(".input-file-container").each(function () {
            var input = $(this).find(".input-file");
            var id = input.attr("id");
            var fileReader = $(this).find(".file-reader");

            $(this).find(".img,img").click(function () {
                fileReader.click()
            });
            input.click(function () {
                fileReader.click()
            });

            fileReader.change(function () {
                var value = $(this).val();
                var latestValue = "";
                for (var i = value.length; i > 0; i--) {
                    if (!/\\/.test(value[i])) {
                        if (value[i]) {
                            latestValue = latestValue + value[i];
                        }
                    } else i = 0;
                }
                var result = "";
                for (var i = latestValue.length; i >= 0; i--) {
                    if (latestValue[i] !== undefined) {
                        result = result + latestValue[i];
                    }
                }

                input.val(result);
            });

        });
    }

    addFileReaderInput();

    $(".change-link-input").click(function (e) {
        $(".file" + $(this).attr('data-id')).toggle()
    });

    $("#guarantor").click(function () {
        $(".info-guarantor").toggle()
    });
    if ($("#guarantor").is(':checked')) {
        $(".info-guarantor").toggle()
    }

    var modal = GetURLParameter('modal');
    if (modal) {
        mr.modals.showModal('#info-modal');
    }

    var pathname = window.location.pathname;
    if (pathname.includes("processFile") || pathname.includes("show-file-with-error")) {
        $('.label-check').addClass("hidden");
    }
});


function GetURLParameter(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            return sParameterName[1];
        }
    }
}

function validate_form() {
    $("#errors").empty();
    $('.invalid').each(function () {
        $(this).removeClass("invalid");
    });

    $("#create-tenant").prop('disabled', true);
    $("#create-tenant").removeClass('btn--primary');
    $("#create-tenant").addClass('btn--disabled');

    $("#create-tenant").attr('data-loading-text', "<i class='fa fa-circle-o-notch fa-spin'></i> Création en cours...");
    $("#create-tenant").button('loading');
    var fileNames = [
        "Pièce d'identité en cours de validité",
        "Justificatif de domicile",
        "Justificatif de situation professionnelle",
        "Avis d'imposition",
        "Justificatif de ressources"
    ];

    var isValid = true;
    $('input[type="text"]').each(function () {
        if ($(this).parent().parent().is(":visible") && $(this).is(":visible")
            && !$(this).hasClass('no-validate') && !$(this).hasClass('special-validation')) {
            if ($.trim($(this).val()) == '') {
                isValid = false;
                $(this).addClass("invalid");
                if ($(this).hasClass("guarantor")) {
                    if ($(this).attr('data-id')) {
                        $("#errors").append("<span class='color--error'>Le champ " + fileNames[$(this).attr('data-id')] + " est manquant dans la partie garant (si vous n'avez pas de garant merci de décocher la case \"J'ai un garant\")</span><br/>")
                    } else {
                        $("#errors").append("<span class='color--error'>Le champ " + $(this).attr("placeholder") + " est manquant dans la partie garant (si vous n'avez pas de garant merci de décocher la case \"J'ai un garant\")</span><br/>")
                    }

                } else {
                    if ($(this).attr('data-id')) {
                        $("#errors").append("<span class='color--error'>Le champ " + fileNames[$(this).attr('data-id')] + " est manquant</span><br/>")

                    } else {

                        $("#errors").append("<span class='color--error'>Le champ " + $(this).attr("placeholder") + " est manquant</span><br/>")
                    }
                }
            } else {
                $(this).removeClass("invalid")
            }
        } else {
            $(this).removeClass("invalid")
        }
    });

    $('input[type="number"]').each(function () {
        if ($(this).parent().parent().is(":visible")
            && !$(this).hasClass('no-validate') && !$(this).hasClass('special-validation')) {
            if ($.trim($(this).val()) == '') {
                isValid = false;
                $(this).addClass("invalid");
                $("#errors").append("<span class='color--error'>Le champ " + $(this).attr("placeholder") + " est manquant</span><br/>")
            } else {
                $(this).removeClass("invalid")
            }
        } else {
            $(this).removeClass("invalid")
        }
    });

    $('input[type="checkbox"].checked-required').each(function () {
        var id = $(this)[0].id;
        var label = $('label[for="' + id + '"]');
        if ($(this).parent().parent().is(":visible") && !$(this).hasClass('no-validate')) {
            if (!$(this).is(":checked")) {
                isValid = false;

                label.addClass("invalid");
                $("#errors").append("<span class='color--error'>Veuillez cocher la case ci-dessus pour continuer</span><br/>")
            } else {
                label.removeClass("invalid");
            }
        } else {
            label.removeClass("invalid");
        }
    });

    $('input[type="password"]').each(function () {
        if ($(this).parent().parent().is(":visible")
            && !$(this).hasClass('no-validate') && !$(this).hasClass('special-validation')) {
            if ($.trim($(this).val()) == '') {
                isValid = false;
                $(this).addClass("invalid");
                $("#errors").append("<span class='color--error'>Le champ " + $(this).attr("placeholder") + " est manquant</span><br/>")
            } else {
                $(this).removeClass("invalid")
            }
        } else {
            $(this).removeClass("invalid")
        }
    });
    var filter = /^([\w-\.\+]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([\w-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/;

    $('input[type="email"]').each(function () {
        if ($(this).parent().parent().is(":visible")
            && !$(this).hasClass('no-validate') && !$(this).hasClass('special-validation')) {
            if ($.trim($(this).val()) == '') {
                $(this).addClass("invalid");
                isValid = false;
                $("#errors").append("<span class='color--error'>Le champ " + $(this).attr("placeholder") + " est manquant</span><br/>")
            } else if (!filter.test($(this).val())) {
                isValid = false;
                $(this).addClass("invalid");
                $("#errors").append("<span class='color--error'>Votre adresse email n'est pas au bon format</span><br/>")

            } else {
                $(this).removeClass("invalid")
            }
        } else {
            $(this).removeClass("invalid")
        }
        // if ($('input.checkbox_check').is(':checked')) {
    });

    var allowedExtensions = ['jpg', 'jpeg', 'gif', 'png', 'pdf'];
    $('input[type="file"]').each(function () {
        if ($(this).parent().parent().is(":visible") && !$(this).hasClass('no-validate') && !$(this).hasClass('special-validation')) {
            var value = $(this).val();

            if (value == '') {
                $(this).addClass("invalid");
                $("#errors").append("<span class='color--error'>Le document est nécessaire</span><br/>")
            }

            file = value.toLowerCase(),
                extension = file.substring(file.lastIndexOf('.') + 1);

            if ($.inArray(extension, allowedExtensions) == -1) {
                $(this).addClass("invalid");
            } else {
                $(this).removeClass("invalid");
            }
        } else {
            $(this).removeClass("invalid")
        }
    });

    $("#create-tenant").removeClass('btn--disabled');
    $("#create-tenant").addClass('btn--primary');
    $("#create-tenant").prop('disabled', false);

    $("#create-tenant").button('reset');
    if ($('.invalid').length > 0) {
        return false;
    }

    if ($('#acceptVerification').length > 0 &&
        !$("#file4Generated").is(":checked") &&
        !$('#acceptVerification').is(":checked")) {

        $('#acceptVerification').addClass('invalid');
        $("#errors").append("<span class='color--error'>Veuillez cocher la case ci-dessus pour continuer</span><br/>");
        return false;
    }

    return isValid;
}


function changePageAndSize() {
    $('.pageSizeSelect').change(function (evt) {
        var url = $(this).attr("data-url");
        var encodedUrl = encodeURI(url);
        var pageSize = encodeURIComponent(this.value);
        window.location.replace(encodedUrl + "?pageSize=" + pageSize + "&page=0");
    });
}

function updateMessagesAdmin(id) {
    $.ajax({
        url: '/bo/message/listNews/' + id,
        type: 'get',
        success: function (data) {

        },
        error: function (data) {
            console.log('error en update :' + data)
        }
    })
}

function updateMessagesAdmin2(id) {
    $.ajax({
        url: '/bo/message/listNews/' + id,
        type: 'get',
        success: function (data) {
        },
        error: function (data) {
            console.log('error en update :' + data)
        }
    })

}



